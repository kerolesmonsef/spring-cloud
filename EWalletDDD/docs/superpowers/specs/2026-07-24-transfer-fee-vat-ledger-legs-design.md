# Transfer fee/vat ledger legs — design

Date: 2026-07-24. Continues the Transfer context (2026-07-21) and Pricing context (2026-07-21) — see `DDD-STUDY.md`. This is the "future ledger posting step" flagged in the Pricing notes, scoped down to audit-only (no balance movement).

## Goal

When a P2P transfer settles, record up to 4 extra `a_transfers` audit rows for the fee/vat split (on top of the existing sender→receiver row), backed by two new well-known accounts (`FEE`, `VAT`). Zero-amount rows are never written. No balance movement in this step — `Account.balance`/`holdBalance` are untouched beyond what `transfer`/`settle` already do today.

**Superseded 2026-07-26**: the sender side of "audit-only" below no longer holds — see `DDD-STUDY.md`'s "Transfer fee/vat real balance movement (sender side)" note. `hold` now reserves `amount+senderFee+senderVat`; `settle` credits `FEE`/`VAT` accounts for real out of that reservation. Receiver-side legs (always zero today) are still audit-only, unchanged. Left the rest of this doc as-written for history.

## Scope decisions (confirmed)

- **Audit-only.** `sender.settle(amount)` / `receiver.deposit(amount)` stay exactly `amount` — no surcharge/deduction applied to real balances yet. Fee/vat legs are bookkeeping rows only, for now. **(Sender side superseded 2026-07-26 — see note above.)**
- **Transfer context orchestrates.** It calls Pricing itself, extracts the 4 fields it needs from `FeeCalculationResult`, and builds its own legs — Pricing stays a pure calculator, no leg-building logic lives there. Same shape will apply to Cashout/Topup when they get this treatment later, each building its own legs from its own transaction type.
- **New array param covers only the 4 fee/vat legs.** The sender→receiver row is unchanged, existing code (`settlementTransaction.addTransfer(sender.id(), receiver.id(), amount)`).
- **Static account lookup uses `AccountReference` itself** (changed from UUID to String) rather than a separate code column — `"FEE"`/`"VAT"` are literal reference values, same field normal accounts use for their random UUID reference.
- **Pricing reached via its own ACL** (`PricingPort` + `PricingAdapter`), mirroring the existing `LedgerTransferPort`/`LedgerTransferAdapter` pattern — consistent with the per-context isolation law already applied to `Rail`/`TransactionType` elsewhere in this codebase.

## Changes

### `AccountReference`: UUID → String

`accounting/domain/valueObject/AccountReference.java`: `record AccountReference(String value)`. `newRef()` = `new AccountReference(UUID.randomUUID().toString())`. Add constants `AccountReference.FEE = new AccountReference("FEE")`, `AccountReference.VAT = new AccountReference("VAT")`.

Ripple (type-only, no logic change beyond dropping now-redundant conversions):
- `AccountJpaEntity.accountReference`: `UUID` → `String` field; drop `@JdbcTypeCode(SqlTypes.CHAR)` (plain `String` maps to the existing `CHAR(36)` column fine — same physical column, ddl-auto=update does nothing here since the column already exists with a compatible type).
- `SpringDataAccountJpa.findByAccountReference(String)` (was `UUID`). Add `existsByAccountReference(String)` for the seeder's idempotency check.
- `Party.internal(...)`: `new Party(reference.value(), type)` — drops `.toString()`.
- `TransactionApplicationService.resolveParty`: `new AccountReference(party.reference())` — drops `UUID.fromString(...)`.

No DB migration needed — existing rows already hold valid UUID text in that column; the Java-side type change doesn't touch stored data.

### `AccountType`: add `FEE`, `VAT`

`accounting/domain/valueObject/AccountType.java` gains two enum constants. **Not** reusing `SYSTEM`: `TransactionApplicationService.loadSystemAccount` does `findByTypeAndCurrency(AccountType.SYSTEM, currency)` expecting exactly one row per currency (the house float) — adding FEE/VAT as `SYSTEM`-typed rows in the same currency would make that query ambiguous. No exhaustive switch exists over `AccountType` today (verified by grep), so adding values is safe.

### `ReferenceDataSeeder`

- `seedType("fee")`, `seedType("vat")` — two new `a_account_types` rows, same helper already used for `"system"`/`"user"`.
- New helper `ensureStaticAccount(AccountReference ref, String accountType, AccountTypeJpaEntity typeRef, CurrencyJpaEntity currency, UserJpaEntity owner)`: idempotent via `accounts.existsByAccountReference(ref.value())`, mirrors `ensureSystemAccount`'s shape (balance/hold = 0, owned by the shared system user). Seeds exactly **one** `FEE` and **one** `VAT` account total, under `defaultCurrency` only — **not** per-currency. `accountReference` carries a DB `unique=true` constraint (single column, not composite with currency), so seeding `"FEE"` once per currency would collide on the second insert. Since these rows are audit-only targets (an `AccountId` to stick on an `a_transfers` row, never a real balance mutation — `TransferJpaEntity.currency` is copied from the leg's `Money`, not cross-checked against the target account's own currency), one global row per static code is enough regardless of which currency the transfer itself is in.

### Accounting application: `TransferLegDTO` + `settle` overload

New `accounting/application/TransferLegDTO.java`:

```java
public record TransferLegDTO(Payer payer, StaticAccount target, Money amount) {
    public enum Payer { SENDER, RECEIVER }
    public enum StaticAccount { FEE, VAT }
}
```

`TransactionApplicationService`:
- `settle(TransactionId txId)` stays (delegates to the new overload with `List.of()`) — Cashout and Topup keep calling the no-args version unchanged.
- New `settle(TransactionId txId, List<TransferLegDTO> feeLegs)`: after the existing `settlementTransaction.addTransfer(sender.id(), receiver.id(), amount)`, loop `feeLegs`, skip `leg.amount().isZero()`, resolve `payer` to the already-loaded `sender`/`receiver` id, resolve `target` to an `AccountId` via a new private `resolveStatic(StaticAccount)` (`accountRepository.findByReference(AccountReference.FEE|VAT).orElseThrow(...)` — no currency parameter, since there's only one global row per static code), call `settlementTransaction.addTransfer(payerId, targetId, leg.amount())`.

### `Money.isZero()`

`shared/domain/Money.java`: `public boolean isZero() { return amount.signum() == 0; }` — used by the zero-guard above.

### Transfer's own Pricing ACL

New `transfer/domain/valueObject/FeeLeg.java` (transfer's own copy, same shape as `TransferLegDTO`):

```java
public record FeeLeg(Payer payer, FeeTarget target, Money amount) {
    public enum Payer { SENDER, RECEIVER }
    public enum FeeTarget { FEE, VAT }
}
```

New `transfer/domain/port/PricingPort.java`:

```java
public interface PricingPort {
    FeeQuote calculateTransferFees(Money amount);
    record FeeQuote(Money senderFeeValue, Money senderVatValue, Money receiverFeeValue, Money receiverVatValue) {}
}
```

New `transfer/infrastructure/pricing/PricingAdapter.java` — the only transfer class importing `pricing.*`: calls `PricingApplicationService.calculateFees(TransactionType.TRANSFER, amount)`, extracts the 4 fields from the returned `FeeCalculationResult` into `PricingPort.FeeQuote`.

### `LedgerTransferPort` / `LedgerTransferAdapter`

`LedgerTransferPort.settle` signature grows: `LedgerSettleRef settle(LedgerHoldRef holdRef, List<FeeLeg> feeLegs)` (only caller is `TransferApplicationService`, so changing the signature directly is safe — no overload needed). `LedgerTransferAdapter` translates each `FeeLeg` to a `TransferLegDTO` and calls `ledger.settle(new TransactionId(holdRef.value()), mapped)`.

### `TransferApplicationService.requestTransfer`

```java
LedgerHoldRef hold = ledger.hold(fromAccount, toAccount, amount);
PricingPort.FeeQuote fees = pricing.calculateTransferFees(amount);
List<FeeLeg> legs = List.of(
    new FeeLeg(SENDER, FEE, fees.senderFeeValue()),
    new FeeLeg(SENDER, VAT, fees.senderVatValue()),
    new FeeLeg(RECEIVER, FEE, fees.receiverFeeValue()),
    new FeeLeg(RECEIVER, VAT, fees.receiverVatValue()));
LedgerSettleRef settle = ledger.settle(hold, legs);
```

New constructor dependency: `PricingPort pricing`.

## Error handling

- Missing `FEE`/`VAT` account row at settle time → `resolveStatic` throws `IllegalArgumentException` (same failure style as `resolveParty`/`loadSystemAccount`) — surfaces as a clear seeding-order bug, not a silent skip.
- Zero-amount legs are silently skipped (not an error) — this is the normal case for the currently-seeded TRANSFER rule (receiver fee/vat = 0).

## Testing

Extend `TransferApplicationServiceIT`: with today's seeded TRANSFER rule (sender 1%, receiver flat 0.00, 5% vat), a transfer produces sender→fee and sender→vat rows (both > 0) but no receiver→fee/vat rows (both zero, skipped). Assert the resulting transaction's `a_transfers` rows — count and shape — via the existing repository/mapper path.

## Out of scope (deferred)

- Real balance movement for fee/vat — explicitly deferred per the "audit-only" scope decision. **Sender side done 2026-07-26** (withdraw sender's surcharge via the inflated hold, credit FEE/VAT accounts for real). Receiver side (deposit receiver's net, i.e. deduct receiver's own fee/vat from what they're credited) is still deferred — moot today since the seeded TRANSFER rule always zeroes receiver fee/vat.
- Applying the same leg-building treatment to Cashout/Topup.
- REST exposure of any of this — no presentation-layer changes.
