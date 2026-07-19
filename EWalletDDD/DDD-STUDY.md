# DDD Study — EWallet (continuation file)

**How to use**: Claude reads this file at session start, continues from "Current status", updates it after each step. Companion learning plan: `todo-learn.md`. Terminology and architecture decisions come from Keroles' strategic-design conversation (context map, ACL, saga) — respect them.

## Business rules (dictated by Keroles — source of truth)

3 subdomains for now:

1. **Accounting / Ledger (CORE)** — tables (all `a_` prefixed): a_users, a_accounts, a_transactions, a_transaction_entries.
   - User has multiple accounts, **one per currency**.
   - Each account: **main balance** + **hold balance**.
   - Cashout lifecycle on ledger: **reserve** (main→hold) → **settle** (hold→0, success) | **release** (hold→main, failure).
2. **Onboarding** — ~5 sequential steps, resume from last completed, profile status "onboarding" until done. Onboarding→Ledger integration: **async event** (OnboardingCompleted → open wallet), never sync.
3. **Cashout** — CashoutId UUID VO. State machine RESERVED → DISPATCHED → CONFIRMED/FAILED (+ PENDING_REVIEW/REJECTED for compliance). ONE `CashoutRailPort`, 3 ACL adapters: **Aani** (UAE NPSS, ISO 20022, sync-ish, irreversible), **LuLu** (async, correspondent, webhook), **Mbank**. Rails normalized to async **inside the adapters** — dispatch always returns PENDING, outcome always arrives as RailConfirmed/RailFailed event. Cashout talks to Ledger only via `LedgerAccountPort` → Ledger's application service (the front door).

## Architecture laws (agreed, apply everywhere)

- Layers per context: `domain/` (pure JDK, no annotations; sub-packages model/valueObject/event/exception/repository — `service/` only if a rule spans aggregates) · `application/` (use cases, @Transactional, no business rules) · `infrastructure/` (persistence, external adapters) · `presentation/` (controllers, DTOs, exception→HTTP mapping).
- Naming: domain events end in `Event`; persistence split into entity/mapper/repository(SpringData)/adapter.
- Only the **Application Service** is exposed to other contexts. Aggregates, domain services, repositories: never.
- Business verbs on aggregates, never setters. Events raised inside the aggregate, pulled and published by the app service.
- Repository save = **Option B**: adapter loads managed JPA entity, copies state onto it → dirty checking UPDATEs, `@Version` optimistic lock works, domain stays version-free.
- Idempotency = guarded state transitions on the aggregate (duplicate settle throws), not ifs in controllers.
- **Tables belong to ONE context, full stop.** Other contexts never SELECT them, never see the JPA entities — only the owning context's application service (or its events). Schema is an implementation detail behind the front door; that's what makes the future microservice split cheap (database-per-service is already true logically).
- **Table prefix = ownership**: Accounting tables are `a_*` (a_users, a_accounts, a_transactions, a_transaction_entries). Cashout will be `c_*`, Onboarding `o_*`. Prefix makes context ownership visible in the schema itself.
- **No shared users table**: Onboarding will get its OWN users table (o_users) with its own shape (steps, KYC, status). Same real-world person, two models, linked by UserId, synced by events. Duplication is deliberate decoupling.
- Enforcement is discipline in a monolith — add an ArchUnit test when Cashout starts: no class outside `accounting..` depends on `accounting.domain..` or `accounting.infrastructure..`; only `accounting.application..` is reachable.

## Roadmap

- [x] **1. Accounting domain layer** (2026-07-17)
- [x] **2. Accounting full layers** — application front door, JPA adapters (Option B + @Version), REST edge, H2 (2026-07-17)
- [x] **3. Cashout context** — CashoutRequest aggregate (state machine), CashoutRailPort + registry, LedgerAccountPort ACL, all 3 rail fakes, c_ persistence, REST edge. Vertical slice with **simulated** callback (confirm/fail app methods). (2026-07-18)
- [ ] 4. Rail webhooks + normalize-to-async in adapters; RailConfirmed/RailFailed → settle/release (saga spine) — replaces the confirm/fail methods
- [ ] 5. Domain events across contexts + Outbox pattern (replace in-process ApplicationEventPublisher)
- [ ] 6. Onboarding context — sequential steps, resume, OnboardingCompleted event → open account
- [ ] 7. Compliance review flow (PENDING_REVIEW threshold via config port)

## Current status

Step 3 done. Cashout context complete through all layers (vertical slice, simulated callback). 24 tests green.

```
com.keroles.ewalletddd/
  shared/domain/                    Money (VO, non-negative, fixed 2dp), Currency (VO: code only — NOT java.util.Currency), UserId
  accounting/
    domain/model/                   Account (aggregate, events, verbs: deposit/withdraw/hold/settle/release),
                                    Transaction (+nested Entry/Type/Status/Direction, restore),
                                    User (minimal aggregate: id+createdAt only — Onboarding owns the rich User)
    domain/valueObject/             AccountId (Long, DB auto-increment), TransactionId (UUID, domain-generated),
                                    AccountReference (UUID), AccountType (SYSTEM/USER/EXTERNAL),
                                    Party (VO: String reference + AccountType — a tx endpoint, incl. external)
    domain/event/                   *Event records: AccountOpenedEvent, MoneyDepositedEvent, MoneyWithdrawnEvent,
                                    FundsHeldEvent, FundsSettledEvent, FundsReleasedEvent
    domain/exception/               InsufficientBalanceException
    domain/repository/              AccountRepository, TransactionRepository, UserRepository (ports)
    (no domain/service/ yet — all rules live in aggregates; add when a rule spans aggregates)
    application/                    Two front doors, split by use-case cluster:
                                    AccountApplicationService — lifecycle+queries: openAccount, getAccount, getUserAccounts
                                    TransactionApplicationService — money movements (each writes the tx record):
                                      deposit, withdraw, reserve→TransactionId, settle(txId), release(txId)
    infrastructure/persistence/
      entity/                       AccountJpaEntity (@Version, FK->a_users), UserJpaEntity, TransactionJpaEntity(+entries)
      mapper/                       AccountMapper, TransactionMapper
      repository/                   SpringData*Jpa interfaces
      adapter/                      Jpa*RepositoryAdapter (Option B save)
    presentation/                   AccountController (open/get/deposit/withdraw — reserve/settle/release NOT exposed publicly),
                                    requests/ (OpenAccountRequest, MoneyRequest), responses/ (AccountResponse),
                                    AccountingExceptionHandler (scoped to this context's controllers → ProblemDetail)
  cashout/
    domain/model/                   CashoutRequest (aggregate + state machine RESERVED→DISPATCHED→CONFIRMED/FAILED,
                                    verbs: markDispatched/confirm/fail, restore)
    domain/valueObject/             CashoutId (UUID), Rail (enum AANI/LULU/MBANK),
                                    LedgerAccountRef (Long), LedgerReservationRef (UUID)
    domain/event/                   CashoutRequestedEvent, CashoutDispatchedEvent, CashoutConfirmedEvent, CashoutFailedEvent
    domain/exception/               IllegalCashoutStateException (extends IllegalStateException → 409)
    domain/repository/              CashoutRepository (port)
    domain/port/                    LedgerAccountPort, CashoutRailPort (+RailDispatchResult), CashoutRailRegistry
    application/                    CashoutApplicationService — THE front door:
                                    requestCashout(account,money,rail)→CashoutId, confirm(id), fail(id), get(id)
    infrastructure/persistence/     entity/mapper/repository/adapter (Option B, c_cashout_requests, @Version)
    infrastructure/ledger/          LedgerAccountAdapter — the ACL, ONLY cashout class importing accounting.*
    infrastructure/rail/            AaniAdapter, LuLuAdapter, MbankAdapter (fakes), SpringCashoutRailRegistry
    presentation/                   CashoutController (POST /cashouts, GET, POST /{id}/confirm|/fail),
                                    requests/ (CreateCashoutRequest), responses/ (CashoutResponse),
                                    CashoutExceptionHandler (scoped)
```

Notes:
- **Reference data (accounting-owned lookup)**: `a_account_types` (seeded `system`, `user`) + `a_currencies` (seeded from `default.currency` + ETH/SOL/BTC). Plain JPA in `accounting/infrastructure/reference/` (entities + Spring Data repos + `ReferenceDataSeeder` CommandLineRunner, existence-checked/idempotent).
- **Account ↔ reference links**: `Account` aggregate carries an `AccountType` enum (valueObject, `SYSTEM`/`USER`/`EXTERNAL`), `Account.open` defaults `USER`. `a_accounts` FKs `currency_id`→a_currencies, `account_type_id`→a_account_types (both nullable — ddl-auto can't back-fill legacy rows; new accounts always set them). FKs resolved at INSERT in `JpaAccountRepositoryAdapter` (findByCode/findByName, cold-path); the `currency`(char3)/`account_type`(name) scalar columns remain the mapper's read source (FK associations never navigated). Unseeded currency at open → `IllegalArgumentException("Unsupported currency: ...")`.
- **Currency is a domain VO, not `java.util.Currency`**: the JDK type is ISO-4217 fiat-only (`getInstance("BTC"/"ETH"/"SOL")` throws), and supported currencies are a business set (a_currencies). `shared/domain/Currency` = `record(code)`; validity enforced at save (adapter's findByCode). `Money` scale is fixed at 2dp (`SCALE` const). Deferred: per-currency precision (crypto 8–18dp) needs `a_currencies.fraction_digits` + wider money columns (currently scale 4).
- **System (house) accounts — repo-only seed (deliberate, per Keroles: "no domain, service and repositories only")**: `ReferenceDataSeeder` builds `AccountJpaEntity` rows DIRECTLY via `SpringDataAccountJpa` (no `Account` aggregate, no app-service method, no domain factory — `openSystem` was removed). One SYSTEM account per seeded currency, balance 1000, holdBalance 0. Idempotent on `(account_type='system', currency)` via `existsByAccountTypeAndCurrency`. All under ONE shared system user (`findFirstByAccountType("system")` reuses the owner → no orphan users on re-run; else creates one `a_users` row). `run()` is `@Transactional` so get-or-create'd type/currency rows stay managed for the account inserts. This is the ONE sanctioned exception to "aggregate is the only door" — pure seed data, infra-to-infra. Deferred: genesis balance has no counterparty transaction (seed shortcut). Owner model = shared system user (alt: nullable `a_accounts.user_id`, blocked by ddl-auto not relaxing NOT NULL).
- **Gotcha fixed (relevant to step 6 open-then-fund)**: the read-only `user_id` mirror on `AccountJpaEntity` (insertable=false) is NOT populated on the in-persistence-context instance right after an INSERT. So opening an account and reloading/re-saving it in the SAME `@Transactional` used to read a null userId → `getReferenceById(null)` NPE. Guarded in `JpaAccountRepositoryAdapter.save` (sets the mirror after insert). Onboarding's "open account + fund on completion" is exactly this shape — the guard makes it safe.
- `openAccount(userId?, currency)`: userId null -> registers new (minimal) User + account; userId given -> must exist. `a_accounts.user_id` is a real FK to `a_users` (JPA: lazy @ManyToOne only for the constraint + read-only mirror `user_id` column for mapping; adapter uses `getReferenceById` so no user SELECT on save).
- **Ids**: AccountId/UserId are Long, DB auto-increment. Consequence: aggregate id is null until first save; adapter calls `assignId()` after INSERT; `AccountOpenedEvent` is raised by the APP SERVICE after save (documented exception to "events raised in aggregate" — the id is born in the DB). TransactionId stays UUID (domain-generated, char(36)).
- `reserve()` creates PENDING ledger Transaction + hold in ONE DB transaction (no double-spend); settle/release complete/fail it — Transaction status guard = idempotency (proven by `duplicateSettleIsRejected` test).
- **Transaction sender/receiver header** (`Party` VO = String reference + AccountType): the *business* parties, incl. external ones (`AccountType.EXTERNAL`, not seeded — no held account). Distinct from `entries` (the internal ledger postings carrying `balanceAfter`) — header names parties entries can't, entries carry balance the header can't. deposit: EXTERNAL→self · withdrawal/reserve: self→EXTERNAL. `a_transactions` gains 4 cols (`sender_reference`/`sender_type`/`receiver_reference`/`receiver_type`). **ponytail:** cashout's real external destination is still the `Party.EXTERNAL` sentinel — thread it through `reserve()` in step 4.
- Events published in-process via ApplicationEventPublisher — replaced by Outbox in step 5.
- DB: MySQL `ddd_ewallet` (root/1234@localhost, `createDatabaseIfNotExist=true`, ddl-auto=update — Flyway later). Tests currently run against that same MySQL (the H2 testRuntime dep is present but `src/test/resources/application.properties` points at MySQL — H2 switch is a TODO).
- `@RequiredArgsConstructor` @SpringBootTest classes need `spring.test.constructor.autowire.mode=all` in `src/test/resources/junit-platform.properties` (read before the context loads — NOT application.properties).
- Tests: `AccountTest`/`CashoutRequestTest` (pure domain), `AccountApplicationServiceIT`/`CashoutApplicationServiceIT` (@SpringBootTest). Cashout IT drives the REAL ledger through the ACL + fake rails: request→hold, confirm→settle, fail→release, double-confirm rejected.

Cashout step-3 specifics:
- **ACL**: `cashout.infrastructure.ledger.LedgerAccountAdapter` is the ONLY cashout class importing `accounting.*`. It depends on `TransactionApplicationService` (reserve/settle/release) — NOT `AccountApplicationService`; the app-service split narrowed cashout's cross-context surface to the movement front door only (it never sees `openAccount`). Plus the `AccountId`/`TransactionId` id VOs as published language. `cashout.domain`/`application` know nothing of accounting. Future ArchUnit rule: forbid outside deps on `accounting.domain.model/event/repository` + `accounting.infrastructure`; permit the id VOs + this one adapter reaching `accounting.application`.
- **Rail extensibility**: `SpringCashoutRailRegistry` self-assembles `Map<Rail,CashoutRailPort>` from all adapter beans. New rail = 1 adapter `@Component` + 1 `Rail` enum value, nothing else.
- **Dispatch outcome is three-way** (`RailDispatchResult.Outcome`): PENDING (async — Aani/LuLu, awaits confirm/fail callback), CONFIRMED (sync — Mbank, settled at dispatch, no callback), REJECTED (refused up front → fail + release). Sync rails walk DISPATCHED→CONFIRMED inside `requestCashout`; the state machine is unchanged, they just don't wait.
- **Reservation link**: `reserve()` returns the ledger `TransactionId` → stored on CashoutRequest as `LedgerReservationRef`; confirm→`settle(ref)`, fail→`release(ref)`.
- **Deferred**: real per-rail webhooks + RailConfirmed/RailFailed saga (step 4, replaces confirm/fail methods); compliance states PENDING_REVIEW/REJECTED (step 7); dispatch-after-commit via Outbox (step 5 — today dispatch runs inside the reserve tx because the fake rail is in-process).

Next session: step 4 — rail webhooks + normalize-to-async saga spine.
