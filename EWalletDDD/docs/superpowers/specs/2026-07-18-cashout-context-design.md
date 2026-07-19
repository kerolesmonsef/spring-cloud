# Cashout Context — Design (step 3, vertical slice)

Date: 2026-07-18
Roadmap: DDD-STUDY.md step 3. Defers step 4 (real webhooks + saga) and step 7 (compliance states).

## Goal

Build the Cashout bounded context end-to-end as a **vertical slice with a simulated
rail callback**: a user requests a cashout, funds are held on the Ledger, the request is
dispatched to a cashout rail (fake), and a later `confirm`/`fail` (standing in for the
async rail callback) settles or releases the hold on the Ledger.

Proves the whole flow — including the anti-corruption layer to Accounting — works, without
building HTTP webhook plumbing to rails that don't exist in a learning environment.

## Decisions (locked)

- **Scope**: vertical slice, sync callback. `confirm(id)`/`fail(id)` app-service methods
  simulate the async rail outcome. Real webhook REST endpoints = step 4.
- **Rails**: all 3 (Aani, LuLu, Mbank) fully written as fakes. `dispatch` returns PENDING
  (accepted, in flight) with a rail reference. A rail may also reject synchronously.
- **ACL coupling**: only `cashout.infrastructure.ledger.LedgerAccountAdapter` imports
  `accounting.application` + `accounting.domain.valueObject`. It is the single sanctioned
  translator. `cashout.domain` and `cashout.application` know **nothing** of `accounting.*`.

## Out of scope (explicit)

- Compliance states PENDING_REVIEW / REJECTED (step 7).
- Real per-rail webhook controllers + `RailConfirmed`/`RailFailed` event-driven saga (step 4).
- Outbox pattern — events stay in-process via `ApplicationEventPublisher` (step 5).
- Cashout destination details (IBAN/wallet) — `dispatch` takes only id + amount for now.

## Package tree (mirrors Accounting)

```
cashout/
  domain/
    model/           CashoutRequest (aggregate + state machine)
    valueObject/     CashoutId (UUID), Rail (enum AANI/LULU/MBANK),
                     LedgerAccountRef (Long), LedgerReservationRef (UUID)
    event/           CashoutRequestedEvent, CashoutDispatchedEvent,
                     CashoutConfirmedEvent, CashoutFailedEvent
    exception/       IllegalCashoutStateException (state-guard)
    repository/      CashoutRepository (port)
    port/            CashoutRailPort, CashoutRailRegistry, LedgerAccountPort   [NEW subpackage]
  application/       CashoutApplicationService  (THE front door)
  infrastructure/
    persistence/
      entity/        CashoutRequestJpaEntity (@Version)
      mapper/        CashoutRequestMapper (toDomain / copyOnto)
      repository/    SpringDataCashoutJpa
      adapter/       JpaCashoutRepositoryAdapter (Option B)
    ledger/          LedgerAccountAdapter  (implements LedgerAccountPort; the ACL)
    rail/            AaniAdapter, LuLuAdapter, MbankAdapter (implement CashoutRailPort),
                     SpringCashoutRailRegistry (implements CashoutRailRegistry)
  presentation/      CashoutController, CashoutExceptionHandler,
                     requests/CreateCashoutRequest, responses/CashoutResponse
```

`domain/port/` is a new subpackage vs Accounting (which only had `repository/` for its
ports). Rationale: `CashoutRailPort`/`LedgerAccountPort` are outbound driven ports, not
repositories. DDD-STUDY sub-package convention updated to allow `port/`.

## Domain — CashoutRequest aggregate

State machine (guards = idempotency, same discipline as `Transaction`):

```
requestCashout ─▶ RESERVED ─▶ DISPATCHED ─▶ CONFIRMED
                     │             └───────▶ FAILED
                     └─(rail rejects at dispatch)──▶ FAILED
```

- Created by factory `CashoutRequest.request(account, amount, rail, reservationRef)` → status
  RESERVED, raises `CashoutRequestedEvent`.
- `markDispatched(railReference)`: RESERVED → DISPATCHED, raises `CashoutDispatchedEvent`.
- `confirm()`: **only** from DISPATCHED → CONFIRMED, raises `CashoutConfirmedEvent`. Illegal
  from any other state → `IllegalCashoutStateException` (rejects duplicate confirm).
- `fail()`: from RESERVED (dispatch rejected) or DISPATCHED → FAILED, raises `CashoutFailedEvent`.
- Fields: `CashoutId id`, `LedgerAccountRef account`, `Money amount`, `Rail rail`,
  `Status status`, `LedgerReservationRef reservationRef`, `String railReference`, `Instant createdAt`.
- Events raised inside the aggregate, pulled + published by the app service (Accounting pattern).
- `restore(...)` factory for the mapper. Business verbs only, no setters.

`CashoutId` is UUID, domain-generated (like `TransactionId`) — no DB round-trip needed, so
(unlike `AccountOpenedEvent`) `CashoutRequestedEvent` is raised inside the aggregate.

## Ports

```java
interface LedgerAccountPort {                       // cashout's terms; adapter translates
    LedgerReservationRef reserve(LedgerAccountRef account, Money amount);
    void settle(LedgerReservationRef reservation);
    void release(LedgerReservationRef reservation);
}

interface CashoutRailPort {
    Rail rail();                                     // which rail this adapter serves
    RailDispatchResult dispatch(CashoutId id, Money amount);
}
record RailDispatchResult(boolean accepted, String railReference, String reason) {
    static RailDispatchResult pending(String ref) { return new RailDispatchResult(true, ref, null); }
    static RailDispatchResult rejected(String reason) { return new RailDispatchResult(false, null, reason); }
}

interface CashoutRailRegistry { CashoutRailPort forRail(Rail rail); }   // built from all adapters
```

`Money` is `shared.domain.Money` (shared kernel — allowed across contexts).

## Application flow

```
CashoutId requestCashout(LedgerAccountRef account, Money amount, Rail rail):
    reservationRef = ledger.reserve(account, amount)                 // hold on Ledger
    cashout = CashoutRequest.request(account, amount, rail, reservationRef)   // RESERVED
    result  = rails.forRail(rail).dispatch(cashout.id(), amount)
    if result.accepted:  cashout.markDispatched(result.railReference())       // DISPATCHED
    else:                cashout.fail(); ledger.release(reservationRef)       // FAILED
    cashoutRepository.save(cashout); publishEvents(cashout); return cashout.id()

void confirm(CashoutId id):   // simulates RailConfirmed callback
    cashout = load(id); cashout.confirm();                           // DISPATCHED→CONFIRMED
    ledger.settle(cashout.reservationRef()); save; publish

void fail(CashoutId id):      // simulates RailFailed callback
    cashout = load(id); cashout.fail();                             // DISPATCHED→FAILED
    ledger.release(cashout.reservationRef()); save; publish

CashoutRequest get(CashoutId id)   // read
```

All methods `@Transactional`. `requestCashout` reserves + dispatches in one method.

**Known ceiling** (`ponytail:`): `dispatch` runs inside the same flow as `reserve`. Real
systems dispatch *after* commit via the outbox (step 5) — you don't hold a DB transaction
open across an external call. Acceptable here because the fake rail dispatch is in-process.

## Rail selection / extensibility

`SpringCashoutRailRegistry` receives `List<CashoutRailPort>` (all `@Component` adapters) and
builds `Map<Rail,CashoutRailPort>` keyed by `CashoutRailPort.rail()`. `forRail` throws if no
adapter serves the rail. **Adding rail #4 = one new `@Component` adapter + one `Rail` enum
value, nothing else changes.** App service depends on the `CashoutRailRegistry` interface
(inward-pointing dependency), not the Spring impl.

The 3 fakes differ only cosmetically (rail name, log line); each returns
`RailDispatchResult.pending(<uuid ref>)`. Aani (sync-ish, irreversible) is the reference
implementation; the registry proves routing.

## Persistence — Option B, table `c_cashout_requests`

Columns: `id char(36) PK`, `account_ref bigint`, `amount decimal(19,4)`, `currency char(3)`,
`rail varchar`, `status varchar`, `ledger_reservation_ref char(36) null`,
`rail_reference varchar null`, `created_at`, `@Version version`.

Adapter save = load managed entity (or new), `copyOnto`, `jpa.save` → dirty-check UPDATE,
`@Version` optimistic lock. Same as `JpaTransactionRepositoryAdapter`.

## ACL — LedgerAccountAdapter (the ONLY cashout class touching accounting.*)

```java
@Component
class LedgerAccountAdapter implements LedgerAccountPort {
    private final AccountApplicationService ledger;   // accounting.application — the front door
    LedgerReservationRef reserve(LedgerAccountRef account, Money amount) {
        TransactionId tx = ledger.reserve(new AccountId(account.value()), amount);
        return new LedgerReservationRef(tx.value());
    }
    void settle(LedgerReservationRef r)  { ledger.settle(new TransactionId(r.value())); }
    void release(LedgerReservationRef r) { ledger.release(new TransactionId(r.value())); }
}
```

Imports `accounting.application.AccountApplicationService` and
`accounting.domain.valueObject.{AccountId, TransactionId}` — the published identifiers.
Nothing else in cashout imports `accounting.*`.

Future ArchUnit rule (when added): forbid outside deps on
`accounting.domain.model/event/repository` and `accounting.infrastructure`; permit the
`accounting.domain.valueObject` ids as published language and exactly this adapter reaching
`accounting.application`.

## Presentation

Cashout REST edge:
- `POST /cashouts` → `requestCashout`, returns `CashoutResponse` (201).
- `GET /cashouts/{id}` → `CashoutResponse`.
- `POST /cashouts/{id}/confirm` and `/fail` → simulate the rail callback (temporary; become
  webhook-driven in step 4).

`CreateCashoutRequest(Long accountId, BigDecimal amount, String currency, String rail)` —
named `Create...` to avoid clashing with the `CashoutRequest` aggregate.
`CashoutResponse(String id, Long accountRef, BigDecimal amount, String currency, String rail,
String status, String reservationRef, String railReference)` with `from(CashoutRequest)`.

`CashoutExceptionHandler` (@RestControllerAdvice): `IllegalCashoutStateException` → 409,
`IllegalArgumentException` (unknown id / rail) → 404 / 400.

## AccountController refactor (asked for; same session)

Extract the inline records:
- `accounting/presentation/requests/OpenAccountRequest`, `MoneyRequest`.
- `accounting/presentation/responses/AccountResponse` (with `from(Account)` → public static).
- `AccountController` imports them; behavior unchanged. Existing `AccountApplicationServiceIT`
  must still pass.

Cashout presentation mirrors this `requests/` + `responses/` layout from the start.

## Tests

- `CashoutRequestTest` — pure domain: happy path RESERVED→DISPATCHED→CONFIRMED; dispatch-reject
  → FAILED; illegal transitions throw (double confirm, confirm before dispatch).
- `CashoutApplicationServiceIT` — `@SpringBootTest` through H2 with the **real** Accounting app
  service + fake rails: open+fund an account, `requestCashout` places a hold (main↓ hold↑),
  `confirm` settles (hold→0), a second `requestCashout` + `fail` releases (hold→main), balances
  asserted at each step, double-confirm rejected.

## DDD-STUDY.md updates (after implementation)

- Mark step 3 done; move "Current status" to Cashout; add the cashout tree.
- Note the `domain/port/` subpackage addition.
- Record the ACL rule refinement for the future ArchUnit test.
