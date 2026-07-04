# DDD Learning Journey — EWallet

Learning DDD by building an e-wallet, step by step. Business first, code last.
Each step gets marked done only after I (the learner) did the exercise and we discussed it.

## Phase 1 — Strategic Design (problem space, zero code)

- [ ] **Step 1: Domain discovery**
  - List all business capabilities (not features, not screens)
  - Exercise: classify each of the 8 capabilities as core / supporting / generic,
    with a one-line reason each
  - Capabilities found so far:
    1. Identity & authentication
    2. Onboarding (multi-step registration, KYC-like, each step has logic)
    3. Wallet / accounting (balances, ledger)
    4. Transactions (record of money movement)
    5. Transfer wallet→wallet
    6. Top-up (money in)
    7. Cash-out (money out via NPSS / LULU Remittance / Mbank — 3rd parties)
    8. Multi-currency (+ FX conversion)

- [ ] **Step 2: Subdomain classification**
  - Core vs supporting vs generic — review my answers, understand the "business dies / limps / shrugs" test
  - Understand why auth and cash-out are commonly misclassified

- [ ] **Step 3: Bounded contexts + ubiquitous language**
  - Draw seams: which capabilities live together, which must be separated
  - Key test: same word, different meaning → different context
    (e.g. "account" in auth vs "account" in ledger)
  - Write a mini-glossary per context

- [ ] **Step 4: Context map**
  - How contexts talk: upstream/downstream, partnership, conformist,
    anti-corruption layer (ACL), open host service, published language
  - Special focus: ACL in front of NPSS / LULU / Mbank integrations

## Phase 2 — Tactical Design (solution space, one context at a time)

- [ ] **Step 5: Aggregates, entities, value objects**
  - Pick ONE context (likely Wallet/Ledger) and model it
  - Invariants decide aggregate boundaries, not data relationships
  - Money as value object, Wallet as aggregate root, etc.

- [ ] **Step 6: Domain events**
  - What happened in the business, past tense: `MoneyDeposited`, `TransferCompleted`
  - Events as the bridge between contexts

- [ ] **Step 7: Repositories, domain services, application services**
  - Repository per aggregate root (not per table)
  - Where logic lives when it fits no single entity

- [ ] **Step 8: Code — package structure**
  - Package-by-context, hexagonal-ish layout inside each context
  - Implement first context end-to-end, then the next

## Phase 3 — Cross-cutting (after first context works)

- [ ] Onboarding as a process (saga / process manager, step logic)
- [ ] Cash-out integrations behind ports + ACL (NPSS, LULU, Mbank adapters)
- [ ] Multi-currency: Money VO with currency, FX as its own concern
- [ ] Consistency: transactions inside aggregate, eventual consistency between aggregates

## Rules to burn into brain

1. DDD starts with the business, never with tables or endpoints.
2. "If I remove this capability — does the business die, limp, or shrug?"
3. Different domain expert / different language → different subdomain.
4. Aggregate boundaries come from invariants, not from foreign keys.
5. A step is done when *I* can explain it back, not when I read it.
