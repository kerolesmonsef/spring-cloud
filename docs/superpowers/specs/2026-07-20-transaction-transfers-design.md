# Transaction transfers — design

Date: 2026-07-20
Module: `EWalletDDD` (Accounting context)

## Goal

Record a ledger-level "who moved money to whom" row for every `Transaction` that
actually completes a money movement — separate from the existing `entries`
(per-account DEBIT/CREDIT postings). Mirrors the user's production `transfer`
table (child of `sva_transactions`), adapted to this project's conventions
(`a_` table prefix, `Money`'s fixed-scale decimal, multi-currency).

## Scope decisions (confirmed with Keroles)

1. **Embedded, not a separate aggregate.** `Transfer` lives inside the
   `Transaction` aggregate exactly like `Entry` does today: no
   `TransferRepository`, no port, no separate application service. Persisted
   via the existing `TransactionRepository` / `TransactionMapper` /
   `TransactionJpaEntity`, same cascade as `entries`.
2. **Every completed movement writes a row**, not just the `transfer()`
   use case: `topup`, `transfer`-settle, and `cashout`-settle all count,
   because `sender_id`/`receiver_id` are just account ids and the system
   account is an `Account` row like any other.

## Data model

New table `a_transfers` (accounting-owned, prefix per project law):

```sql
CREATE TABLE a_transfers (
  id             BIGINT NOT NULL AUTO_INCREMENT,
  transaction_id CHAR(36) NOT NULL,
  sender_id      BIGINT NOT NULL,
  receiver_id    BIGINT NOT NULL,
  amount         DECIMAL(19,4) NOT NULL,
  currency       VARCHAR(3) NOT NULL,
  position       INT,
  PRIMARY KEY (id),
  KEY transfer_transaction_id__fk (transaction_id),
  CONSTRAINT transfer_transaction_id__fk FOREIGN KEY (transaction_id) REFERENCES a_transactions (id)
);
```

Deviations from the pasted prod DDL, both mirroring how `a_transaction_entries`
already deviates from raw prod shape:
- `amount` is `DECIMAL(19,4)` not `DECIMAL(32,0)` — this project's `Money` is
  fixed at 2dp; scale 0 would truncate cents.
- Added `currency` — needed to reconstruct the `Money` VO on read; entries
  carries the same column for the same reason.
- `transaction_id` is `CHAR(36)` (UUID) not `BIGINT` — `TransactionId` is a
  domain-generated UUID in this project, matching `a_transactions.id`.
- `position` — Hibernate-managed (`@OrderColumn`), added automatically by
  `ddl-auto=update`, same as the existing (undocumented in the prod DDL)
  ordering column on `a_transaction_entries`.

No FK on `sender_id`/`receiver_id`, matching `entries.account_id`'s existing
plain-column style (not navigated, just an id).

## Domain layer (`Transaction.java`)

```java
public record Transfer(AccountId senderId, AccountId receiverId, Money amount) {}

private final List<Transfer> transfers = new ArrayList<>();

public void addTransfer(AccountId senderId, AccountId receiverId, Money amount) {
    if (status != Status.COMPLETED)
        throw new IllegalStateException("Cannot add a transfer to a " + status + " transaction");
    transfers.add(new Transfer(senderId, receiverId, amount));
}

public List<Transfer> transfers() { return List.copyOf(transfers); }
```

`restore(...)` gains a `List<Transfer> transfers` parameter (appended after
existing `entries` param), mirroring how `entries` is threaded through today.

The `COMPLETED`-only guard is the enforcement point for "insert on success
only, never pending, never failed" — it's a domain invariant, not just
application-service discipline.

## Persistence layer

`TransactionJpaEntity`:
- New nested `@Entity @Table(name = "a_transfers") TransferJpaEntity` (fields:
  `id`, `senderId`, `receiverId`, `amount`, `currency`) — same shape/style as
  `TransactionEntryJpaEntity`.
- New `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) @JoinColumn(name = "transaction_id", nullable = false) @OrderColumn(name = "position") List<TransferJpaEntity> transfers` field, identical wiring to `entries`.

`TransactionMapper`:
- `toDomain`: maps `row.getTransfers()` into `Transaction.Transfer` list, passed into `restore(...)`.
- `copyOnto`: append-only loop for transfers (same pattern as the existing entries loop — only add rows the JPA entity doesn't have yet).
- New private helpers `transferToDomain` / `transferToRow`, mirroring `entryToDomain` / `entryToRow`.

## Application layer (`TransactionApplicationService`)

Exactly two call sites, both **after** `.complete()` (guard requires COMPLETED):

- `topup()`: `tx.addTransfer(system.id(), user.id(), amount);`
- `settle()`: `settlement.addTransfer(sender.id(), receiver.id(), hold.amount());`
  — **not** on `hold`. `hold.complete()` also runs during `settle()` (it's the
  bookkeeping transition that closes out the HOLD row), but the `hold` row
  itself never represents money landing anywhere — only the new `settlement`
  row does. Adding a transfer there too would double-record one movement.

No change to `cashout()` (still PENDING, no `.complete()` call) or `release()`
(`releaseTx.complete()` runs, but a release is a reversal, not a successful
movement — no transfer row).

## Testing

Add to `AccountApplicationServiceIT` (autowire `TransactionRepository`):
- `topup` → `transactionRepository.findById(txId).transfers()` has exactly 1
  entry (system → user, correct amount).
- `transfer` hold-only (no settle yet) → hold transaction's `transfers()` is
  empty.
- `transfer` + `settle` → the **settlement** transaction's `transfers()` has
  exactly 1 entry (from → to); the hold transaction's stays empty.
- `cashout` + `release` → both hold and release transactions have empty
  `transfers()`.

These are the existing IT's flows with one more assertion each — no new test
class.

## Out of scope

- No REST exposure of transfers (mirrors `entries` — not surfaced via
  `presentation/` today either).
- No backfill for historical rows (new table, nothing to backfill).
- No change to `cashout()`/hold creation, `release()`, or the entries logic.
