# DDD Study — EWallet (continuation file)

**How to use**: Claude reads this file at session start, continues from "Current status", updates it after each step. Companion learning plan: `todo-learn.md`. Terminology and architecture decisions come from Keroles' strategic-design conversation (context map, ACL, saga) — respect them.

## Business rules (dictated by Keroles — source of truth)

3 subdomains for now:

1. **Accounting / Ledger (CORE)** — tables (all `a_` prefixed): a_users, a_accounts, a_transactions, a_transaction_entries.
   - User has multiple accounts, **one per currency**.
   - Each account: **main balance** + **hold balance**.
   - Cashout lifecycle on ledger: **cashout** (user hold, PENDING, receiver=SYSTEM) → **settle** (hold→0 + system deposit + system CREDIT entry, COMPLETED) | **release** (hold→main, FAILED).
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
- **Table prefix = ownership**: Accounting tables are `a_*` (a_users, a_accounts, a_transactions, a_transaction_entries). Cashout is `c_*`, Topup `t_*` (t_topup_requests), Transfer `tr_*` (tr_transfers — `t_` was already Topup's), Onboarding `o_*`. Prefix makes context ownership visible in the schema itself.
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
- [x] **8. Pricing context** — no aggregate, read-only fee config (p_fee_charges) + pure calculation domain service (2026-07-21)

## Current status

Step 3 done. Cashout context complete through all layers (vertical slice, simulated callback). Ledger movements are topup/transfer/cashout (SYSTEM-backed). **Topup context** added 2026-07-20 (same rail/state-machine skeleton as Cashout, but **no hold** — see note below). **Transfer context** added 2026-07-21 (P2P, no rail, hold+settle atomic in one call — see note below). **Pricing context** added 2026-07-21 (no aggregate — read-only fee config + calculation domain service, see note below). Tests green.

```
com.keroles.ewalletddd/
  shared/domain/                    Money (VO, non-negative, fixed 2dp), Currency (VO: code only — NOT java.util.Currency), UserId
  accounting/
    domain/model/                   Account (aggregate, events, verbs: deposit/withdraw/hold/settle/release),
                                    Transaction (+nested Entry/Transfer/Type/Status/Direction, restore),
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
                                      topup(user, amount)          system.withdraw + user.deposit, 2 entries, COMPLETED
                                      transfer(from, to, amount)   from.withdraw + to.deposit, 2 entries, COMPLETED
                                      cashout(user, amount)→txId   user.hold, 1 entry (user DEBIT), PENDING, receiver=SYSTEM
                                      settle(txId)                 user.settle + system.deposit + system CREDIT entry, COMPLETED
                                      release(txId)                user.release only, FAILED
                                      loadSystemAccount(currency)  throws IllegalStateException if missing
    infrastructure/persistence/
      entity/                       AccountJpaEntity (@Version, FK->a_users), UserJpaEntity, TransactionJpaEntity(+entries+transfers)
      mapper/                       AccountMapper, TransactionMapper
      repository/                   SpringData*Jpa interfaces (+ findByAccountTypeAndCurrency for house-account resolve)
      adapter/                      Jpa*RepositoryAdapter (Option B save; findByTypeAndCurrency mirrors existsBy…)
    presentation/                   AccountController (open/get/topup/transfer — cashout/settle/release NOT public REST;
                                    leave-wallet money is cashout via CashoutController, not a direct withdraw),
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
  topup/
    domain/model/                   TopupRequest (aggregate + state machine PENDING→COMPLETED/FAILED,
                                    verbs: recordDispatch/complete/recordLedgerRef/fail, restore)
    domain/valueObject/             TopupId (UUID), Rail (enum TCS/MBANK),
                                    LedgerAccountRef (Long), LedgerTransactionRef (UUID)
    domain/event/                   TopupRequestedEvent, TopupDispatchedEvent, TopupCompletedEvent, TopupFailedEvent
    domain/exception/               IllegalTopupStateException (→ 409)
    domain/repository/              TopupRepository (port)
    domain/port/                    LedgerTopupPort, TopupRailPort, TopupRailRegistry, RailDispatchResult
    application/                    TopupApplicationService — THE front door:
                                    requestTopup(account,money,rail)→TopupId, confirm(id), fail(id), get(id)
    infrastructure/persistence/     entity/mapper/repository/adapter (Option B, t_topup_requests, @Version)
    infrastructure/ledger/          LedgerTopupAdapter — the ACL, ONLY topup class importing accounting.*
    infrastructure/rail/            TcsAdapter (async→PENDING), MbankAdapter (sync→CONFIRMED, bean "topupMbankAdapter"),
                                    SpringTopupRailRegistry
    presentation/                   TopupController (POST /topups, GET, POST /{id}/confirm|/fail),
                                    requests/ (CreateTopupRequest), responses/ (TopupResponse),
                                    TopupExceptionHandler (scoped)
  transfer/
    domain/model/                   Transfer (aggregate, no state machine — complete()/restore() only,
                                    hold+settle already resolved by the time it's constructed)
    domain/valueObject/             TransferId (UUID), LedgerAccountRef (Long, own copy),
                                    LedgerHoldRef (UUID), LedgerSettleRef (UUID)
    domain/event/                   TransferCompletedEvent
    domain/repository/              TransferRepository (port)
    domain/port/                    LedgerTransferPort (hold(from,to,amount), settle(holdRef))
    application/                    TransferApplicationService — THE front door:
                                    requestTransfer(from,to,amount)→TransferId (hold+settle in one
                                    @Transactional call, self-transfer guarded), get(id)
    infrastructure/persistence/     entity(TransferRequestJpaEntity)/mapper/repository/adapter
                                    (Option B, tr_transfers, @Version)
    infrastructure/ledger/          LedgerTransferAdapter — the ACL, ONLY transfer class importing
                                    accounting.*; calls the existing TransactionApplicationService.transfer
    presentation/                   TransferController (POST /transfers, GET /{id}),
                                    requests/ (CreateTransferRequest), responses/ (TransferResponse),
                                    TransferExceptionHandler (scoped)
  pricing/
    domain/valueObject/            TransactionType (own copy: TOPUP/TRANSFER/CASHOUT), FeeType (VALUE/PERCENTAGE),
                                    FeeChargeRule (transactionType, senderFee+Type, receiverFee+Type, vatPercentage;
                                    static zero(transactionType) — all-zero rule, used when no config row exists),
                                    FeeCalculationResult (senderTotalAmount, receiverTotalAmount, senderFeeValue,
                                    receiverFeeValue, senderVatValue, receiverVatValue — all Money;
                                    senderFees()/receiverFees() = fee+vat per side)
    domain/repository/             FeeChargeRepository (port: findByTransactionType → FeeChargeRule, never empty)
    domain/service/                FeeCalculationService — pure calc, no Spring, branches on rule.transactionType():
                                    TOPUP → senderFeeValue forced zero (sender is the SYSTEM account, never charged),
                                    CASHOUT → receiverFeeValue forced zero (receiver is the SYSTEM account),
                                    TRANSFER → both sides charged as configured. VALUE fee is flat, PERCENTAGE fee =
                                    amount * fee/100; vat = feeValue * vatPercentage/100 (per side). senderTotalAmount
                                    = amount + senderFeeValue + senderVatValue (surcharge, what sender pays out);
                                    receiverTotalAmount = amount - receiverFeeValue - receiverVatValue (deduction,
                                    what receiver nets) — these two feed the future ledger posting step (sender→fee,
                                    sender→vat, receiver→fee, receiver→vat movements)
    application/                    PricingApplicationService — THE front door: calculateFees(transactionType,
                                    amount) → FeeCalculationResult; loads the rule (never throws — an unconfigured
                                    transactionType resolves to FeeChargeRule.zero(), i.e. a free transaction),
                                    delegates to the domain service
    infrastructure/persistence/     entity(FeeChargeJpaEntity)/mapper(FeeChargeMapper.toDomain(transactionType, row)
                                    — row null → FeeChargeRule.zero(transactionType))/repository(SpringDataFeeChargeJpa)/
                                    adapter (JpaFeeChargeRepositoryAdapter implements FeeChargeRepository) —
                                    read-only, p_fee_charges, no @Version (config data, not an aggregate)
    (no presentation/ yet — nothing asked for a REST edge)
```

Notes:
- **Reference data (accounting-owned lookup)**: `a_account_types` (seeded `system`, `user`) + `a_currencies` (seeded from `default.currency` + ETH/SOL/BTC). Plain JPA in `accounting/infrastructure/reference/` (entities + Spring Data repos + `ReferenceDataSeeder` CommandLineRunner, existence-checked/idempotent).
- **Account ↔ reference links**: `Account` aggregate carries an `AccountType` enum (valueObject, `SYSTEM`/`USER`/`EXTERNAL`), `Account.open` defaults `USER`. `a_accounts` FKs `currency_id`→a_currencies, `account_type_id`→a_account_types (both nullable — ddl-auto can't back-fill legacy rows; new accounts always set them). FKs resolved at INSERT in `JpaAccountRepositoryAdapter` (findByCode/findByName, cold-path); the `currency`(char3)/`account_type`(name) scalar columns remain the mapper's read source (FK associations never navigated). Unseeded currency at open → `IllegalArgumentException("Unsupported currency: ...")`.
- **Currency is a domain VO, not `java.util.Currency`**: the JDK type is ISO-4217 fiat-only (`getInstance("BTC"/"ETH"/"SOL")` throws), and supported currencies are a business set (a_currencies). `shared/domain/Currency` = `record(code)`; validity enforced at save (adapter's findByCode). `Money` scale is fixed at 2dp (`SCALE` const). Deferred: per-currency precision (crypto 8–18dp) needs `a_currencies.fraction_digits` + wider money columns (currently scale 4).
- **System (house) accounts — repo-only seed (deliberate, per Keroles: "no domain, service and repositories only")**: `ReferenceDataSeeder` builds `AccountJpaEntity` rows DIRECTLY via `SpringDataAccountJpa` (no `Account` aggregate, no app-service method, no domain factory — `openSystem` was removed). One SYSTEM account per seeded currency, **genesis balance `1_000_000_000`**, holdBalance 0. Why so large: test funding switched from unlimited EXTERNAL deposit to `topup` (drawn from the shared system float); ITs commit to MySQL and the seeder is idempotent (won't refill), so a small float depletes within/across runs → random `InsufficientBalanceException`. Idempotent on `(account_type='system', currency)` via `existsByAccountTypeAndCurrency`. All under ONE shared system user (`findFirstByAccountType("system")` reuses the owner → no orphan users on re-run; else creates one `a_users` row). `run()` is `@Transactional` so get-or-create'd type/currency rows stay managed for the account inserts. This is the ONE sanctioned exception to "aggregate is the only door" — pure seed data, infra-to-infra. Deferred: genesis balance has no counterparty transaction (seed shortcut). Owner model = shared system user (alt: nullable `a_accounts.user_id`, blocked by ddl-auto not relaxing NOT NULL).
- **Gotcha fixed (relevant to step 6 open-then-fund)**: the read-only `user_id` mirror on `AccountJpaEntity` (insertable=false) is NOT populated on the in-persistence-context instance right after an INSERT. So opening an account and reloading/re-saving it in the SAME `@Transactional` used to read a null userId → `getReferenceById(null)` NPE. Guarded in `JpaAccountRepositoryAdapter.save` (sets the mirror after insert). Onboarding's "open account + fund on completion" is exactly this shape — the guard makes it safe.
- `openAccount(userId?, currency)`: userId null -> registers new (minimal) User + account; userId given -> must exist. `a_accounts.user_id` is a real FK to `a_users` (JPA: lazy @ManyToOne only for the constraint + read-only mirror `user_id` column for mapping; adapter uses `getReferenceById` so no user SELECT on save).
- **Ids**: AccountId/UserId are Long, DB auto-increment. Consequence: aggregate id is null until first save; adapter calls `assignId()` after INSERT; `AccountOpenedEvent` is raised by the APP SERVICE after save (documented exception to "events raised in aggregate" — the id is born in the DB). TransactionId stays UUID (domain-generated, char(36)).
- **Ledger money flows** (every user-facing movement is internal↔internal; SYSTEM is one side of topup/cashout):
  | flow | direction | balance verbs | entries | status |
  |---|---|---|---|---|
  | `topup` | system → user | system.withdraw + user.deposit | 2 (system DEBIT, user CREDIT) | COMPLETED atomic |
  | `transfer` | user → user | from.withdraw + to.deposit | 2 | COMPLETED atomic |
  | `cashout` | user → system | user.hold only | 1 (user DEBIT) | PENDING |
  | `settle` | (cashout success) | user.settle + system.deposit | append system CREDIT → 2 | COMPLETED |
  | `release` | (cashout fail) | user.release only | still 1 | FAILED |
  `cashout`/`settle`/`release` run in ONE DB transaction each (no double-spend); Transaction status guard = idempotency (proven by `duplicateSettleIsRejected`). `loadSystemAccount` crashes with `IllegalStateException` if the house account is missing for that currency.
- **Transaction types**: `TOPUP`, `TRANSFER`, `CASHOUT` are the live app-service paths. `DEPOSIT`/`WITHDRAWAL` factories + `Party.EXTERNAL` remain on the aggregate for true outside-world legs (kept deliberately; not used by the app service anymore). Cashout receiver is the real SYSTEM party — this retires the old `Party.EXTERNAL` sentinel on the cashout path.
- **Transaction sender/receiver header** (`Party` VO = String reference + AccountType): the *business* parties, incl. external ones (`AccountType.EXTERNAL`, not seeded — no held account). Distinct from `entries` (the internal ledger postings carrying `balanceAfter`) — header names parties entries can't, entries carry balance the header can't. Live headers: topup system→user · transfer user→user · cashout user→system. `a_transactions` has 4 cols (`sender_reference`/`sender_type`/`receiver_reference`/`receiver_type`).
- **`transfers`** (new, 2026-07-20): third child list on `Transaction`, alongside `entries` — `Transfer(senderId: AccountId, receiverId: AccountId, amount: Money)`, table `a_transfers` (FK→`a_transactions`, no FK on sender/receiver ids, same style as `entries.account_id`). Embedded like `entries`: no separate repository/port/service, persisted via `TransactionRepository`/`TransactionMapper`/`TransactionJpaEntity` only. `Transaction.addTransfer(...)` **throws unless `status == COMPLETED`** — the "insert on success only" business rule is a domain invariant, not app-service discipline. Written at exactly 2 call sites in `TransactionApplicationService`: `topup()` (system→user, one-shot) and `settle()` on the **settlement** row (sender→receiver, the resolved hold's parties) — never on the `hold` row itself (its own `.complete()` during settle is bookkeeping, not a money-landing event) and never on `release()` (a reversal, not a success). `topup()`/`settle()` now return `TransactionId` (`topup` was `void`) so callers can look up the row that got the transfer. Spec: `docs/superpowers/specs/2026-07-20-transaction-transfers-design.md`.
- **LazyInitializationException fix**: `entries`/`transfers` stay LAZY (default) — no fetch-policy change. Reading a `Transaction` via `TransactionRepository.findById` outside an open Hibernate session (e.g. a test with no `@Transactional`) threw `LazyInitializationException` on the mapper's collection reads — a latent bug on `entries` since it was added, surfaced once the transfers tests exercised that path. Fixed narrowly: `JpaTransactionRepositoryAdapter.findById` is now `@Transactional(readOnly = true)`, so `TransactionMapper.toDomain` (which reads both collections) always runs inside an open session, regardless of the caller's tx context. Two eager `@OneToMany` lists on one entity was considered and rejected — cartesian/extra-select risk on any future multi-row query (`findAll`, "list a user's transactions") for a fix that only needed to matter at the single-row read.
- Events published in-process via ApplicationEventPublisher — replaced by Outbox in step 5.
- DB: MySQL `ddd_ewallet` (root/1234@localhost, `createDatabaseIfNotExist=true`, ddl-auto=update — Flyway later). Tests currently run against that same MySQL (the H2 testRuntime dep is present but `src/test/resources/application.properties` points at MySQL — H2 switch is a TODO).
- `@RequiredArgsConstructor` @SpringBootTest classes need `spring.test.constructor.autowire.mode=all` in `src/test/resources/junit-platform.properties` (read before the context loads — NOT application.properties).
- Tests: `AccountTest`/`CashoutRequestTest` (pure domain), `AccountApplicationServiceIT`/`CashoutApplicationServiceIT` (@SpringBootTest). Funding helper uses `topup`; hold path uses `cashout`. Transfer IT: fund A, transfer to B, assert both balances. Cashout IT drives the REAL ledger through the ACL + fake rails: request→hold, confirm→settle, fail→release, double-confirm rejected.

Cashout step-3 specifics:
- **ACL**: `cashout.infrastructure.ledger.LedgerAccountAdapter` is the ONLY cashout class importing `accounting.*`. It depends on `TransactionApplicationService` (`cashout`/`settle`/`release` — port still says `reserve` in cashout language; adapter translates `reserve→ledger.cashout`) — NOT `AccountApplicationService`; the app-service split narrowed cashout's cross-context surface to the movement front door only (it never sees `openAccount`). Plus the `AccountId`/`TransactionId` id VOs as published language. `cashout.domain`/`application` know nothing of accounting. Future ArchUnit rule: forbid outside deps on `accounting.domain.model/event/repository` + `accounting.infrastructure`; permit the id VOs + this one adapter reaching `accounting.application`.
- **Rail extensibility**: `SpringCashoutRailRegistry` self-assembles `Map<Rail,CashoutRailPort>` from all adapter beans. New rail = 1 adapter `@Component` + 1 `Rail` enum value, nothing else.
- **Dispatch outcome is three-way** (`RailDispatchResult.Outcome`): PENDING (async — Aani/LuLu, awaits confirm/fail callback), CONFIRMED (sync — Mbank, settled at dispatch, no callback), REJECTED (refused up front → fail + release). Sync rails walk DISPATCHED→CONFIRMED inside `requestCashout`; the state machine is unchanged, they just don't wait.
- **Reservation link**: `LedgerAccountPort.reserve` → `TransactionApplicationService.cashout` returns the ledger `TransactionId` → stored on CashoutRequest as `LedgerReservationRef`; confirm→`settle(ref)`, fail→`release(ref)`.
- **Deferred**: real per-rail webhooks + RailConfirmed/RailFailed saga (step 4, replaces confirm/fail methods); compliance states PENDING_REVIEW/REJECTED (step 7); dispatch-after-commit via Outbox (step 5 — today dispatch runs inside the cashout/reserve tx because the fake rail is in-process).

Topup context (2026-07-20) — spec `docs/superpowers/specs/2026-07-20-topup-context-design.md`:
- **Money system → user, credit-on-confirm (no hold).** Deliberate contrast with Cashout, and the main lesson: Cashout holds the user's *own* funds so they can't double-spend while the rail works; Topup holds **nothing**, because the money isn't the user's until the external rail delivers it. So the Ledger is touched **only on success** — never reserved up front. Reuses the existing one-shot `TransactionApplicationService.topup` (system.withdraw + user.deposit, COMPLETED atomic); **zero Accounting changes**.
- **State machine `PENDING → COMPLETED | FAILED`** (no RESERVED/DISPATCHED — nothing to reserve, and dispatch is same-tx as request for the in-process fake). Guard `complete() requires PENDING` = idempotency (duplicate callback throws).
- **Guard-before-ledger** (advisor-flagged, matches the codebase's fail-fast rule): `complete()` takes no arg and can throw *before* `ledger.topup()` moves money; the resulting id is attached after via `recordLedgerRef()` (no-state-change setter). Payoff is the **sequential** duplicate callback: the guard throws before `ledger.topup` runs, so no wasted ledger tx + rollback. Proven by `TopupDuplicateCallbackTest` (pure fakes, asserts `ledger.topup` invoked exactly once) — the IT `doubleConfirmIsRejected_creditedOnce` can't prove it (its `@Transactional` rollback would undo a guard-last double-credit and leave the same balance either way; it only shows the duplicate is rejected + credited once end-to-end). The **concurrent** double-callback is a different guard: both callers pass the in-memory check and `@Version` optimistic lock rolls one back.
- **Rails**: `Mbank` sync (dispatch→CONFIRMED, credit now, no callback), `Tcs` async (dispatch→PENDING, credit at confirm callback). Own `Rail`/`RailDispatchResult`/`LedgerAccountRef` copies (per-context isolation law). `SpringTopupRailRegistry` self-assembles by `rail()`.
- **Bean-name gotcha**: both cashout and topup have a class `MbankAdapter` → same default bean name `mbankAdapter` → `ConflictingBeanDefinitionException`. Fixed by `@Component("topupMbankAdapter")` on topup's; routing is by `rail()` so the bean name is functionally irrelevant.
- **ACL**: `topup.infrastructure.ledger.LedgerTopupAdapter` is the ONLY topup class importing `accounting.*` (depends on `TransactionApplicationService` + the id VOs). Same future-ArchUnit rule as Cashout's adapter.
- Tests: `TopupRequestTest` (pure domain, state machine), `TopupApplicationServiceIT` (@SpringBootTest, real ledger + fake rails: Mbank credits at dispatch, Tcs pending→confirm credits, Tcs fail leaves balance untouched, double-confirm credited once), `TopupRejectAtDispatchTest` (pure fakes, REJECTED branch the real fakes can't reach).

Transfer context (2026-07-21):
- **P2P, no rail, no hold/settle split in time.** `TransactionApplicationService.transfer(from,to,amount)` and its generic `settle`/`release` (already handling `TRANSFER` type alongside `CASHOUT`) predate this context — Transfer's ACL just calls `ledger.transfer` for hold then `ledger.settle` for settle, back to back, inside ONE `@Transactional` app-service method. No external rail means no PENDING outcome is possible, so there's nothing to wait on — either both ledger calls succeed and the aggregate is persisted COMPLETED, or an exception (e.g. `InsufficientBalanceException`) rolls the whole transaction back and nothing is persisted. No `release()` call needed on the Transfer port — there is no code path that holds and then decides not to settle.
- **`Transfer` aggregate has no state machine**: unlike `CashoutRequest`/`TopupRequest`, it's constructed already-complete via `Transfer.complete(from,to,amount,holdRef,settleRef)` — both refs are always present (both columns `NOT NULL` on `tr_transfers`, unlike Cashout's nullable `ledgerSettleRef`). `restore()` rehydrates from persistence; no other verbs.
- **Self-transfer guard lives in the application service**, before the ledger call (`if (fromAccount.equals(toAccount)) throw new IllegalArgumentException(...)`) — not in the aggregate, because the aggregate doesn't exist yet at that point and the check must happen before the ledger does two loads of what could be the same account. Precedent: `TransactionApplicationService`'s own `loadAccount`/`loadSystemAccount` guards also live in application, not domain, in this codebase.
- **Removed** `AccountController.transfer` (`POST /accounts/{id}/transfer/{toId}`) — it only called `ledger.transfer` (hold) with no way to ever settle or release it, so funds could get stuck in `holdBalance` forever. `TransferController` (`POST /transfers`) is now the only way to move money user→user.
- **ACL**: `transfer.infrastructure.ledger.LedgerTransferAdapter` is the ONLY transfer class importing `accounting.*`.
- **Naming collision avoided**: the JPA entity is `TransferRequestJpaEntity`, not `TransferJpaEntity` — accounting already has a `TransferJpaEntity` (the embedded `a_transfers` audit row on `Transaction`, see the `transfers` note above), and Hibernate entity names must be distinct even across packages.
- Tests: `TransferTest` (pure domain — `complete()` sets fields + raises one event, `restore()` raises none), `TransferApplicationServiceIT` (`@SpringBootTest`, real ledger: happy path moves both balances and settles the hold immediately, insufficient balance throws, self-transfer throws).

Pricing context (2026-07-21):
- **No aggregate — `p_fee_charges` IS the whole model.** Unlike Cashout/Topup/Transfer (state machines with lifecycle), Pricing has nothing to save/transition; it's a rate table plus a pure calculator. So there's no `domain/model/`, no `Option B save`, no `@Version` — just a read-only `domain/repository/` port + `domain/service/` calculator, which is more layering than accounting's own reference data uses (AccountType/Currency skip the port and are read directly by infra) but matches the aggregate-context shape for consistency, since `PricingApplicationService` needed a domain abstraction to depend on rather than a JPA entity.
- **Own `TransactionType` copy** (`TOPUP`/`TRANSFER`/`CASHOUT`) — same per-context isolation law as Cashout/Topup's own `Rail` copies. Does NOT reference `accounting.domain.model.Transaction.Type` (crossing that boundary is forbidden — only the accounting application service is reachable from outside).
- **Seeded from `accounting/infrastructure/reference/ReferenceDataSeeder.java` directly** (per explicit instruction), not a Pricing-owned seeder — `SpringDataFeeChargeJpa` injected alongside the existing raw repos, idempotent via `existsByTransactionType`. This is a deliberate extension of the already-sanctioned "infra-to-infra, pure seed data" exception (used for system accounts) — now crossing a context boundary (accounting infra → pricing infra) rather than staying within one, so it's a slightly bigger exception than before. Seeded 3 rows, one per type, mixing VALUE/PERCENTAGE across sender/receiver: TOPUP (sender 1.5% / receiver flat 1.00), CASHOUT (sender flat 5.00 / receiver 0.5%), TRANSFER (sender 1% / receiver flat 0.00) — all at 5% VAT.
  **Stale since the sender/receiver-exempt-side business rule below was added**: `FeeCalculationService` now force-zeroes `senderFeeValue` for TOPUP and `receiverFeeValue` for CASHOUT regardless of config, so the seeded TOPUP sender fee (1.50%) and CASHOUT receiver fee (0.5%) are dead values — never applied. Not yet cleaned up in the seeder; harmless (ignored, not wrong-charged) but worth zeroing there too for honesty.
- **`FeeType.VALUE` fees must be seeded at ≤2dp** — `Money`'s constructor (`shared/domain/Money.java`) calls `setScale(2)` with no rounding mode, so an unrounded flat fee throws `ArithmeticException` the first time it's calculated. The domain service also explicitly rounds every computed value (`HALF_UP`, scale 2) before wrapping in `Money`, since the `PERCENTAGE` path can otherwise produce more than 2 decimals.
- **No currency column on `p_fee_charges`** (per the given schema) — a flat `VALUE` fee is a bare number, applied in whatever currency the priced `Money` amount already carries. Fine for this single-default-currency demo; would need a currency column if fees ever needed to differ by currency.
- Tests: `FeeCalculationServiceTest` (pure domain, no Spring) — one case per transaction type proving the exempt side is force-zeroed (TOPUP: sender free, CASHOUT: receiver free, TRANSFER: both charged) plus a `FeeChargeRule.zero()` case (unconfigured type ⇒ free). `FeeChargeMapperTest` — no row found ⇒ `FeeChargeRule.zero(transactionType)`.
- **Deferred**: no REST edge (nothing asked); no `PENDING_REVIEW`-style config port; `totalAmount` formula is a judgment call (see domain/service note above) — flag if the intended shape differs.

Next session: step 4 — rail webhooks + normalize-to-async saga spine.
