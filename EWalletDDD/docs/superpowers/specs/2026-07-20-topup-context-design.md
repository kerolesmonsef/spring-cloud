# Topup Context — Design (vertical slice)

Date: 2026-07-20
Roadmap: new context, same shape as Cashout (DDD-STUDY.md step 3). Defers real webhooks + saga
(the Cashout step-4 work applies here too) and the Outbox (step 5).

## Goal

Build the Topup bounded context end-to-end as a **vertical slice with a simulated rail
callback**: a user requests a topup (money **system → user**), the request is stored PENDING and
dispatched to a topup rail (fake). A **sync** rail (Mbank) credits the Ledger immediately; an
**async** rail (Tcs) stays PENDING until a later `confirm`/`fail` (standing in for the rail
callback) credits the Ledger or marks the topup failed.

Proves the flow — including the ACL to Accounting — works, and deliberately contrasts with
Cashout: **Topup places no hold.** The money isn't the user's until the rail delivers it, so the
Ledger is touched **only on success**, never up front.

## Decisions (locked)

- **Ledger interaction = credit-on-confirm (Option B).** Reuse the existing one-shot
  `TransactionApplicationService.topup(user, amount)` — **zero Accounting changes**. Credit runs
  once, on success (Mbank at dispatch, Tcs at callback). On failure the Ledger is never touched
  (user was never credited — nothing to undo). No `reserve`/`settle`/`release`, no system-float
  hold.
- **State machine = `PENDING → COMPLETED | FAILED`.** Drops Cashout's `RESERVED`/`DISPATCHED`:
  no hold to reserve, and dispatch is same-tx as request for the in-process fake, so a separate
  state buys nothing observable. Guard `complete() requires PENDING` = idempotency (duplicate
  callback throws).
- **Guard before ledger, always.** `complete()` (the state guard) runs and can throw *before*
  `ledger.topup()` moves any money. The ledger id it produces is attached afterward via a
  no-state-change setter. This preserves the "fail fast before touching accounts" discipline the
  rest of the codebase follows (`Transaction.complete()` in `settle`, `CashoutRequest.confirm()`).
- **Rails**: 2 fakes — `Tcs` (async → PENDING, awaits callback) and `Mbank` (sync → CONFIRMED at
  dispatch, no callback). A rail may also reject synchronously (REJECTED → FAILED).
- **Own copies** of `Rail`, `RailDispatchResult`, `LedgerAccountRef`, `LedgerTransactionRef` in
  the topup package — deliberate per-context isolation (DDD-STUDY architecture law), not
  accidental duplication with Cashout.
- **ACL coupling**: only `topup.infrastructure.ledger.LedgerTopupAdapter` imports `accounting.*`.
  `topup.domain` and `topup.application` know nothing of `accounting.*`.

## Out of scope (explicit)

- Real per-rail webhook controllers + event-driven saga — `confirm`/`fail` app-service methods
  simulate the async callback (same deferral as Cashout step 4).
- Outbox — events stay in-process via `ApplicationEventPublisher` (step 5).
- Compliance / review states.
- Topup funding-source details (card, bank ref) — `dispatch` takes only id + amount for now.
- Per-currency system-float accounting for the genesis balance (pre-existing seed shortcut).

## Package tree (mirrors Cashout, prefix `t_`)

```
topup/
  domain/
    model/           TopupRequest (aggregate + state machine, restore)
    valueObject/     TopupId (UUID), Rail (enum TCS/MBANK),
                     LedgerAccountRef (Long), LedgerTransactionRef (UUID)
    event/           TopupRequestedEvent, TopupDispatchedEvent,
                     TopupCompletedEvent, TopupFailedEvent
    exception/       IllegalTopupStateException (state-guard -> 409)
    repository/      TopupRepository (port)
    port/            LedgerTopupPort, TopupRailPort, TopupRailRegistry, RailDispatchResult
  application/       TopupApplicationService  (THE front door)
  infrastructure/
    persistence/
      entity/        TopupRequestJpaEntity (@Version)
      mapper/        TopupRequestMapper (toDomain / copyOnto)
      repository/    SpringDataTopupJpa
      adapter/       JpaTopupRepositoryAdapter (Option B)
    ledger/          LedgerTopupAdapter  (implements LedgerTopupPort; the ACL)
    rail/            TcsAdapter, MbankAdapter (implement TopupRailPort),
                     SpringTopupRailRegistry (implements TopupRailRegistry)
  presentation/      TopupController, TopupExceptionHandler,
                     requests/CreateTopupRequest, responses/TopupResponse
```

## Domain — TopupRequest aggregate

State machine (guards = idempotency):

```
requestTopup ─▶ PENDING ─▶ COMPLETED
                   └──────▶ FAILED
```

- Factory `TopupRequest.request(account, amount, rail)` → status PENDING, raises
  `TopupRequestedEvent`. No ledger ref yet.
- `recordDispatch(railReference)`: stores the rail reference, **stays PENDING**, raises
  `TopupDispatchedEvent`. Needed so an async (Tcs) callback can correlate back to this row.
- `complete()`: **guard** PENDING → COMPLETED, raises `TopupCompletedEvent`. Illegal from any
  other state → `IllegalTopupStateException` (rejects duplicate confirm). Takes **no argument** —
  the ledger id does not exist yet at guard time.
- `recordLedgerRef(LedgerTransactionRef)`: pure setter, **no state change, no event** — the
  audit link from this topup to the Ledger transaction it produced. Called right after
  `ledger.topup(...)` returns.
- `fail(String reason)`: guard PENDING → FAILED, raises `TopupFailedEvent`.
- Fields: `TopupId id`, `LedgerAccountRef account`, `Money amount`, `Rail rail`, `Status status`,
  `String railReference` (null until dispatched), `LedgerTransactionRef ledgerTransactionRef`
  (null until completed), `Instant createdAt`.
- Events raised inside the aggregate, pulled + published by the app service.
- `restore(...)` factory for the mapper. Business verbs only, no setters.

`TopupId` is UUID, domain-generated (like `CashoutId`/`TransactionId`) — no DB round-trip, so
`TopupRequestedEvent` is raised inside the aggregate.

Note `ledgerTransactionRef` is **write-only within Topup** (audit link only) — unlike Cashout's
`reservationRef`, nothing in Topup reads it back, because a topup is terminal at COMPLETED.

## Ports

```java
interface LedgerTopupPort {                          // topup's terms; ACL adapter translates
    LedgerTransactionRef topup(LedgerAccountRef account, Money amount);
}

interface TopupRailPort {
    Rail rail();                                      // which rail this adapter serves
    RailDispatchResult dispatch(TopupId id, Money amount);
}

record RailDispatchResult(Outcome outcome, String railReference, String reason) {
    enum Outcome { PENDING, CONFIRMED, REJECTED }
    static RailDispatchResult pending(String ref)     { return new RailDispatchResult(PENDING, ref, null); }
    static RailDispatchResult confirmed(String ref)   { return new RailDispatchResult(CONFIRMED, ref, null); }
    static RailDispatchResult rejected(String reason) { return new RailDispatchResult(REJECTED, null, reason); }
}

interface TopupRailRegistry { TopupRailPort forRail(Rail rail); }   // built from all adapters
```

`Money` is `shared.domain.Money` (shared kernel — allowed across contexts).

## Application flow

```
TopupId requestTopup(LedgerAccountRef account, Money amount, Rail rail):
    topup  = TopupRequest.request(account, amount, rail)          // PENDING, ledger untouched
    result = rails.forRail(rail).dispatch(topup.id(), amount)
    switch result.outcome():
        PENDING   -> topup.recordDispatch(result.railReference())            // Tcs: await callback
        CONFIRMED -> topup.recordDispatch(result.railReference());           // Mbank sync:
                     topup.complete();                                       //   guard FIRST
                     topup.recordLedgerRef(ledger.topup(account, amount))    //   then credit + link
        REJECTED  -> topup.fail(result.reason())                            // ledger never touched
    topupRepository.save(topup); publishEvents(topup); return topup.id()

void confirm(TopupId id):   // simulates RailConfirmed callback
    topup = load(id)
    topup.complete();                                             // guard FIRST — throws before money moves
    topup.recordLedgerRef(ledger.topup(topup.account(), topup.amount()));   // credit now
    save; publish

void fail(TopupId id):      // simulates RailFailed callback
    topup = load(id); topup.fail("rail reported failure");        // nothing to undo — never credited
    save; publish

TopupRequest get(TopupId id)   // read
```

All methods `@Transactional`. `requestTopup` requests + dispatches in one method.

**Known ceiling** (`ponytail:`): `dispatch` runs inside the same flow/tx as request. Real systems
dispatch *after* commit via the outbox (step 5) — you don't hold a DB transaction open across an
external call. Acceptable here because the fake rail dispatch is in-process.

## Rail selection / extensibility

`SpringTopupRailRegistry` receives `List<TopupRailPort>` (all `@Component` adapters) and builds
`Map<Rail, TopupRailPort>` keyed by `TopupRailPort.rail()`. `forRail` throws if no adapter serves
the rail. **Adding rail #3 = one new `@Component` adapter + one `Rail` enum value.** App service
depends on the `TopupRailRegistry` interface, not the Spring impl.

- `MbankAdapter` → `RailDispatchResult.confirmed(ref)` (sync: final outcome now, no callback).
- `TcsAdapter` → `RailDispatchResult.pending(ref)` (async: awaits confirm/fail callback).

## Persistence — Option B, table `t_topup_requests`

Columns: `id char(36) PK`, `account_ref bigint`, `amount decimal(19,4)`, `currency char(3)`,
`rail varchar(10)`, `status varchar(20)`, `rail_reference varchar(100) null`,
`ledger_transaction_ref char(36) null`, `created_at`, `@Version version`.

Adapter save = load managed entity (or new), `copyOnto`, `jpa.save` → dirty-check UPDATE,
`@Version` optimistic lock. Same as `JpaCashoutRepositoryAdapter`.

## ACL — LedgerTopupAdapter (the ONLY topup class touching accounting.*)

```java
@Component
class LedgerTopupAdapter implements LedgerTopupPort {
    private final TransactionApplicationService ledger;   // accounting.application — movement front door
    LedgerTransactionRef topup(LedgerAccountRef account, Money amount) {
        TransactionId tx = ledger.topup(new AccountId(account.value()), amount);
        return new LedgerTransactionRef(tx.value());
    }
}
```

Imports `accounting.application.TransactionApplicationService` and
`accounting.domain.valueObject.{AccountId, TransactionId}` — the published identifiers. Nothing
else in topup imports `accounting.*`. Same rule as Cashout's `LedgerAccountAdapter`; the future
ArchUnit rule covers this adapter too.

## Presentation

Topup REST edge:
- `POST /topups` → `requestTopup`, returns `TopupResponse` (201).
- `GET /topups/{id}` → `TopupResponse`.
- `POST /topups/{id}/confirm` and `/fail` → simulate the rail callback (temporary; become
  webhook-driven when the Cashout step-4 work lands).

`CreateTopupRequest(Long accountId, BigDecimal amount, String currency, String rail)` — named
`Create...` to avoid clashing with the `TopupRequest` aggregate.
`TopupResponse(String id, Long accountRef, BigDecimal amount, String currency, String rail,
String status, String railReference, String ledgerTransactionRef)` with `from(TopupRequest)`.

`TopupExceptionHandler` (@RestControllerAdvice, scoped to this context's controller):
`IllegalTopupStateException` → 409, `IllegalArgumentException` (unknown id / rail) → 404 / 400.

## Tests

- `TopupRequestTest` — pure domain: happy path PENDING → COMPLETED; PENDING → FAILED; illegal
  transitions throw (double complete, complete after fail, fail after complete).
- `TopupApplicationServiceIT` — `@SpringBootTest` through the **real** Accounting app service +
  fake rails:
  - open an account; **Mbank** `requestTopup` → status COMPLETED, user balance up by amount,
    a Ledger COMPLETED transaction exists, `ledgerTransactionRef` set.
  - **Tcs** `requestTopup` → status PENDING, balance **unchanged**, `ledgerTransactionRef` null;
    `confirm` → COMPLETED, balance up.
  - **Tcs** `requestTopup` → PENDING; `fail` → FAILED, balance unchanged.
  - double-`confirm` rejected (`IllegalTopupStateException`).

## DDD-STUDY.md updates (after implementation)

- Add the Topup context to the tree and "Current status".
- Record the credit-on-confirm decision and the **Topup-vs-Cashout contrast** (no hold — money
  isn't the user's until the rail delivers) as a teaching note.
- Add `t_` to the table-prefix ownership list.
- Note `guard-before-ledger` ordering with the `complete()` / `recordLedgerRef()` split.
