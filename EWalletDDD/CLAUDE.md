# CLAUDE.md — EWalletDDD

DDD learning project (Eric Evans style), built step by step with Keroles.

**Read `DDD-STUDY.md` FIRST, every session.** It is the continuation file: business rules (source of truth), architecture laws, roadmap, and current status. Continue from "Current status" and update the file after each completed step. Companion learning plan: `todo-learn.md`.

Quick facts (details in DDD-STUDY.md):
- Bounded contexts: Accounting/Ledger (core, done), Cashout (next), Onboarding.
- Layers per context: `domain/` (pure JDK) · `application/` (the only front door) · `infrastructure/` (persistence, adapters) · `presentation/` (controllers).
- Tables are context-owned, prefixed by context: `a_*` Accounting, `c_*` Cashout, `o_*` Onboarding.
- DB: MySQL `ddd_ewallet` root/1234@localhost, ddl-auto=update. Run tests: `./gradlew test` (needs local MySQL).
- **No comments in Java code.** No `//` or `/* */`. Code must be self-documenting.
