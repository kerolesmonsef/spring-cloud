# DDD Study — EWallet (continuation file)

**How to use**: Claude reads this file at session start, continues from "Current status", updates it after each step. Companion learning plan: `todo-learn.md`. Terminology and architecture decisions come from Keroles' strategic-design conversation (context map, ACL, saga) — respect them.

## Business rules (dictated by Keroles — source of truth)

3 subdomains for now:

1. **Accounting / Ledger (CORE)** — tables (all `a_` prefixed): a_users, a_accounts, a_transactions, a_transaction_entries.
   - User has multiple accounts, **one per currency**.
   - Each account: **main balance** + **hold balance**.
   - Cashout lifecycle on ledger: **reserve** (main→hold) → **settle** (hold→0, success) | **release** (hold→main, failure).
2. **Onboarding** — ~5 sequential steps, resume from last completed, profile status "onboarding" until done. Onboarding→Ledger integration: **async event** (OnboardingCompleted → open wallet), never sync.
3. **Cashout** — CashoutId UUID VO. State machine RESERVED → DISPATCHED → CONFIRMED/FAILED (+ PENDING_REVIEW/REJECTED for compliance). ONE `PayoutRailPort`, 3 ACL adapters: **Aani** (UAE NPSS, ISO 20022, sync-ish, irreversible), **LuLu** (async, correspondent, webhook), **Mbank**. Rails normalized to async **inside the adapters** — dispatch always returns PENDING, outcome always arrives as RailConfirmed/RailFailed event. Cashout talks to Ledger only via `LedgerAccountPort` → Ledger's application service (the front door).

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
- [ ] 3. Cashout context — CashoutRequest aggregate (state machine), PayoutRailPort, LedgerAccountPort, Aani adapter first
- [ ] 4. Rail webhooks + normalize-to-async in adapters; RailConfirmed/RailFailed → settle/release (saga spine)
- [ ] 5. Domain events across contexts + Outbox pattern (replace in-process ApplicationEventPublisher)
- [ ] 6. Onboarding context — sequential steps, resume, OnboardingCompleted event → open account
- [ ] 7. Compliance review flow (PENDING_REVIEW threshold via config port)

## Current status

Step 2 done. Accounting context complete through all layers, 13 tests green.

```
com.keroles.ewalletddd/
  shared/domain/                    Money (VO, non-negative), UserId
  accounting/
    domain/model/                   Account (aggregate, events, verbs: deposit/withdraw/hold/settle/release),
                                    Transaction (+nested Entry/Type/Status/Direction, restore),
                                    User (minimal aggregate: id+createdAt only — Onboarding owns the rich User)
    domain/valueObject/             AccountId (Long, DB auto-increment), TransactionId (UUID, domain-generated),
                                    AccountReference (UUID)
    domain/event/                   *Event records: AccountOpenedEvent, MoneyDepositedEvent, MoneyWithdrawnEvent,
                                    FundsHeldEvent, FundsSettledEvent, FundsReleasedEvent
    domain/exception/               InsufficientBalanceException
    domain/repository/              AccountRepository, TransactionRepository, UserRepository (ports)
    (no domain/service/ yet — all rules live in aggregates; add when a rule spans aggregates)
    application/                    AccountApplicationService — THE front door:
                                    openAccount, deposit, withdraw, reserve→TransactionId, settle(txId), release(txId)
    infrastructure/persistence/
      entity/                       AccountJpaEntity (@Version, FK->a_users), UserJpaEntity, TransactionJpaEntity(+entries)
      mapper/                       AccountMapper, TransactionMapper
      repository/                   SpringData*Jpa interfaces
      adapter/                      Jpa*RepositoryAdapter (Option B save)
    presentation/                   AccountController (open/get/deposit/withdraw — reserve/settle/release NOT exposed publicly),
                                    AccountingExceptionHandler (domain exceptions → ProblemDetail)
```

Notes:
- `openAccount(userId?, currency)`: userId null -> registers new (minimal) User + account; userId given -> must exist. `a_accounts.user_id` is a real FK to `a_users` (JPA: lazy @ManyToOne only for the constraint + read-only mirror `user_id` column for mapping; adapter uses `getReferenceById` so no user SELECT on save).
- **Ids**: AccountId/UserId are Long, DB auto-increment. Consequence: aggregate id is null until first save; adapter calls `assignId()` after INSERT; `AccountOpenedEvent` is raised by the APP SERVICE after save (documented exception to "events raised in aggregate" — the id is born in the DB). TransactionId stays UUID (domain-generated, char(36)).
- `reserve()` creates PENDING ledger Transaction + hold in ONE DB transaction (no double-spend); settle/release complete/fail it — Transaction status guard = idempotency (proven by `duplicateSettleIsRejected` test).
- Events published in-process via ApplicationEventPublisher — replaced by Outbox in step 5.
- DB: MySQL `ddd_ewallet` (root/1234@localhost, `createDatabaseIfNotExist=true`, ddl-auto=update — Flyway later). Tests: H2 in-memory MySQL mode via `src/test/resources/application.properties`.
- Tests: `AccountTest` (pure domain), `AccountApplicationServiceIT` (@SpringBootTest slice through H2, proves UPDATE-not-INSERT).

Next session: step 3 — Cashout context.
