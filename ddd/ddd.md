# Domain-Driven Design — What It Is, and Why

This document teaches DDD as **Eric Evans** describes it in *Domain-Driven Design: Tackling
Complexity in the Heart of Software* (2003). The companion file
[`PROJECT-STRUCTURE.md`](./PROJECT-STRUCTURE.md) walks through how this project applies every
idea below to a school (teacher / student / course) example.

> The code in this project intentionally has **no comments**. The teaching lives in these two
> documents; the code stays clean and reads as the design.

---

## 1. The one idea everything else serves

> **Software for a complex business should be built around a model of that business — a model
> that the developers and the domain experts share, speak, and refine together.**

Most of DDD is machinery in service of that sentence. Evans splits the machinery into two halves:

- **Strategic design** — how you carve a large domain into pieces and how those pieces relate.
  This is the *most important and most overlooked* half. It is where DDD pays for itself.
- **Tactical design** — the building blocks (Entity, Value Object, Aggregate, Repository, …)
  you use *inside* one piece to keep the model rich and honest.

A common failure (including the earlier version of this very example — see
`PROJECT-STRUCTURE.md`) is to learn only the tactical half and skip the strategic half. You get
tidy Value Objects inside one giant undifferentiated model, and you never see the real benefit.

---

## 2. Ubiquitous Language

The model is expressed in a **Ubiquitous Language**: one vocabulary, used identically in
conversation, in documents, and in code. If the business says "a teacher **authors** a course and
later **publishes** it," then the code says `course.author(...)` and `course.publish()` — not
`courseService.create(...)` and `setStatus(1)`.

The payoff: when a domain expert says a sentence, you can almost point at the line of code it maps
to. Translation layers between "how the business talks" and "how the code is named" are where bugs
and misunderstandings breed.

A subtle but central point: **a word's meaning is only stable inside a boundary.** "Course" means
something different to the marketing/authoring side than to the learning side. That observation
leads directly to the most important strategic pattern.

---

## 3. Strategic design (the half people skip)

### 3.1 Subdomains — not all of the business is equal

A real business divides into **subdomains**, and they deserve very different levels of investment:

| Subdomain type | What it is | How much you invest |
|---|---|---|
| **Core domain** | The thing that makes *this* business special and worth money | Your best people, full tactical DDD, constant refinement |
| **Supporting subdomain** | Necessary, specific to you, but not your edge | Modest effort, simpler modeling |
| **Generic subdomain** | Every business needs it; nobody wins by doing it bespoke (auth, billing, email) | **Buy or adopt** — don't lovingly hand-craft it |

This is **distillation**: spend modeling effort where it matters, and deliberately *don't*
elsewhere. In this project the core domain is **Enrollment/Learning**; **Catalog** is supporting;
**Billing** and **Identity** are generic (and intentionally not built — see the project doc).

### 3.2 Bounded Context — the central strategic pattern

A **Bounded Context** is a boundary within which a single model — and a single meaning for each
term in the Ubiquitous Language — applies and stays consistent.

The key realization: **the same word can name two different models in two different contexts, and
that is correct, not a bug to fix.** Trying to force one all-purpose model across the whole company
produces a bloated object that is wrong everywhere. (Concretely, see the "Course means two
different things" section of `PROJECT-STRUCTURE.md`.)

A bounded context is *not* the same as a subdomain. A subdomain is a problem-space slice (how the
business is organized); a bounded context is a solution-space slice (how your software/model is
organized). They often line up, but you choose the contexts.

### 3.3 Context Map — how contexts relate

Two contexts that need to cooperate are connected, and the *kind* of connection is a real design
decision. A **Context Map** names those relationships. The common ones:

- **Shared Kernel** — a small, shared sub-model two teams agree to keep in lockstep (use sparingly;
  every shared type is a coupling both teams must coordinate on).
- **Customer / Supplier** — a downstream context depends on an upstream one; upstream commits to
  serving downstream's needs.
- **Conformist** — downstream just accepts upstream's model as-is (no negotiating power).
- **Anti-Corruption Layer (ACL)** — downstream builds a translation layer so the upstream model
  *never leaks into* downstream's own model. The downstream context keeps its language clean.
- **Published Language** — a well-documented shared format for exchange between contexts.

This project uses a **Customer/Supplier** link (Enrollment is downstream of Catalog) implemented
through an **Anti-Corruption Layer**, so the core context never imports the supplier's classes.

---

## 4. Tactical design (the building blocks inside one context)

### 4.1 Entity
An object defined by **identity and continuity over time**, not by its attribute values. Two
students named "Grace Hopper" are different students. Entities carry **behavior**, not just data.

### 4.2 Value Object
An object defined **entirely by its attributes**, with **no identity**. `Money(49.99)` equals any
other `Money(49.99)`. Value Objects are:
- **immutable** — operations return new instances, they never mutate;
- **self-validating** — an invalid one cannot be constructed (`Rating.of(9)` throws);
- the cure for **primitive obsession** — a price is `Money`, not a bare `double`.

### 4.3 Aggregate and Aggregate Root
An **Aggregate** is a cluster of objects treated as **one unit for data changes**, with a single
**Aggregate Root** as the only entry point. The aggregate is a **consistency boundary**: every
invariant that must *always* hold is enforced within one aggregate, in one transaction.

Three rules that keep aggregates healthy (Evans, sharpened by Vaughn Vernon):
1. **Protect true invariants inside the boundary** — only rules that must be instantly consistent.
2. **Keep aggregates small** — prefer many small aggregates over one large one. A large aggregate
   is slow to load, contends under concurrency, and accumulates unrelated rules.
3. **Reference other aggregates by identity (ID), not by object reference.** This keeps boundaries
   crisp and aggregates loadable in isolation. Consistency *across* aggregates is achieved later
   (eventually), often via domain events — not inside a single transaction.

> The single most common DDD mistake is the **god aggregate**: one root that owns several
> unrelated lifecycles. It violates rule 2, drags in rule-3 violations, and forces awkward
> workarounds. This project exists partly to show the fix — see `PROJECT-STRUCTURE.md`.

### 4.4 Repository
A collection-like abstraction for **retrieving and persisting aggregates** — and there is **one
repository per aggregate root**, never one per table. Crucially, the repository **interface lives
in the domain** (expressed in domain terms), while its **implementation lives in infrastructure**
(JPA, SQL, etc.). The domain depends on the abstraction; the framework depends on the domain. That
is the Dependency Inversion that keeps the domain pure.

### 4.5 Factory
When creating an object (or whole aggregate) is complex or must guarantee invariants, encapsulate
that creation in a **Factory** — typically a static factory method on the aggregate root
(`Course.author(...)`). Construction is a domain responsibility, so an object cannot be born
invalid.

### 4.6 Domain Service
Some operations are genuinely **not** the responsibility of any single Entity or Value Object —
they're a domain concept in their own right, often spanning multiple aggregates. Model these as a
**Domain Service**: a stateless operation, named in the Ubiquitous Language, living in the domain.
(Distinct from an *Application Service* — see §5.) Example here: `EnrollmentPolicy`, which decides
whether an enrollment may be opened given catalog state and existing enrollments.

### 4.7 Specification
A **Specification** is a small object that encapsulates a **boolean business rule** —
"is this candidate satisfactory?" — as a first-class, named, testable thing
(`ReviewableCourseSpecification.isSatisfiedBy(student, course)`). It keeps a rule out of a tangle
of `if` statements and lets the rule be reused and tested in isolation.

### 4.8 Domain Event
Something **meaningful that happened** in the domain, named in the **past tense**
(`StudentEnrolledEvent`). Events let one part of the system react to another **without the first
knowing the second exists** — the key to decoupling, and the natural way to reach consistency
*across* aggregates and contexts.

---

## 5. The layers (and where each rule goes)

DDD is usually run inside a layered (or hexagonal) architecture where **dependencies point
inward**, toward the domain:

```
        Interfaces      REST controllers — accept input, return DTOs, no business logic
            │
        Application     Application Services — orchestrate a use case: load aggregate,
            │           invoke domain, save, publish events, map DTOs. A thin coordinator.
            ▼
          Domain        Entities, Value Objects, Aggregates, Domain Services, Specifications,
            ▲           Repository *interfaces*, Domain Events. The model. No framework leaking in.
            │
      Infrastructure    Repository *implementations*, messaging, the ACL adapter. Depends on
                        the domain's interfaces, never the reverse.
```

**Application Service vs Domain Service** — the distinction people most often blur:

- A **Domain Service** expresses a *business* operation. It belongs to the model and is named in
  the Ubiquitous Language. It contains domain logic.
- An **Application Service** expresses an *application use case*. It owns the transaction, calls
  repositories and domain objects in the right order, publishes events, and maps to DTOs. It
  contains **no business rules of its own** — it coordinates.

A blunt placement guide for a check or rule:

| The rule… | …belongs in |
|---|---|
| is pure validation of one value (`rating 1–5`, `money ≥ 0`) | **Value Object** |
| is an invariant of one aggregate (`progress can't go backwards`) | **Entity / Aggregate Root** |
| is a business decision spanning aggregates (`may this enrollment open?`) | **Domain Service** |
| is a reusable yes/no business predicate (`is this course reviewable?`) | **Specification** |
| is "do these steps in this order, in a transaction" | **Application Service** |
| is "translate the other context's model into ours" | **Anti-Corruption Layer (infrastructure)** |

---

## 6. Advantages of DDD

1. **The model and the code stay in sync** — Ubiquitous Language means a business sentence maps
   to a line of code; conversations and code don't drift apart.
2. **Business rules can't be bypassed** — invariants live in aggregates and value objects, not
   scattered across services where a caller can forget them.
3. **Complexity is divided, not piled up** — bounded contexts let large systems (and large teams)
   work without one model collapsing under conflicting meanings.
4. **Right effort in the right place** — distillation focuses your best work on the core domain and
   lets you buy the generic parts.
5. **The domain is testable in isolation** — pure domain objects test with plain JUnit, no Spring,
   no database (this project demonstrates it).
6. **Boundaries map to services** — aggregates and bounded contexts are natural seams if you later
   split into microservices.
7. **Change is localized** — a new reaction to an event is a new listener, not a surgical edit to
   existing code.

## 7. Drawbacks / costs of DDD

1. **More moving parts** — value objects, DTOs, repository interfaces + adapters, events,
   domain/application service split. More files than a CRUD app.
2. **A real learning curve** — aggregates, context mapping, and the service distinction take time
   and a team that agrees to them.
3. **Overkill for simple CRUD** — if the app is "save a form and read it back," DDD is pure
   overhead. Use it where there is genuine business complexity.
4. **Aggregate design is genuinely hard** — too big = the god-aggregate trap; too small = invariants
   you can't enforce in one transaction. It takes iteration.
5. **Cross-aggregate / cross-context queries need deliberate design** — you can't just join across
   boundaries; you use query methods, read models, or CQRS.
6. **It only works with buy-in** — one developer reaching past an aggregate root or leaking one
   context's model into another quietly dissolves the benefits.

## 8. When to use it — and when not

**Use DDD when:** the domain has real, evolving business rules; multiple people/teams share the
codebase; the system is long-lived; the business logic — not the plumbing — is the hard part.

**Don't use DDD when:** it's basic CRUD, a throwaway prototype, or a purely technical pipeline with
no rich domain; or when the deadline genuinely leaves no room for modeling and the domain is
simple.

**The pragmatic middle (Evans' own advice):** apply full tactical DDD to the **core domain**, keep
**supporting** subdomains simple, and **buy** the **generic** ones. Not every part of a system
deserves — or should get — the same treatment.

---

## 9. Further reading

- Eric Evans — *Domain-Driven Design* (2003), the source.
- Vaughn Vernon — *Implementing Domain-Driven Design* and *Domain-Driven Design Distilled*
  (practical, modern aggregate rules).
- Vlad Khononov — *Learning Domain-Driven Design* (2021, approachable, strong on strategic design).
- Martin Fowler's DDD articles — free, online.
