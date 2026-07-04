# The School Example — Project Structure

This project models a small **online school**: teachers author courses, students enroll in
published courses, progress through them, and review them. It is built to demonstrate
Domain-Driven Design as described in [`ddd.md`](./ddd.md) — read that first for the concepts; this
file shows how they land in code.

Stack: Spring Boot 4.1, Java 17, Spring Data JPA, H2 in-memory. Base package `com.keroles.ddd`.

---

## 1. Subdomain analysis — deciding what matters

Before writing any class, classify the business (`ddd.md` §3.1):

| Subdomain | Type | Why | Built here? |
|---|---|---|---|
| **Enrollment / Learning** | **Core** | A school lives or dies on the learning experience: who can enroll, progress, reviews. This is the differentiator. | **Yes — full tactical DDD** |
| **Catalog** | **Supporting** | Authoring and publishing courses is necessary and specific to us, but not the edge. | **Yes — modeled simply** |
| **Billing / Payments** | **Generic** | Every business charges money; nobody wins by hand-rolling it. | **No — "buy, don't build."** Deliberately omitted to demonstrate distillation. |
| **Identity / Accounts** | **Generic** | Auth is a solved, buy-it problem. | No |

> Dropping Billing is itself a DDD decision, not laziness. The earlier version of this example
> hand-built Payment into the core model; distillation says don't. Catalog still raises
> `CoursePublishedEvent` on publish — an intentional **extension point**: it's the exact seam a
> future Billing or Notification context would subscribe to, without catalog ever knowing they
> exist. (It has no subscriber today on purpose; `StudentEnrolledEvent` shows the full
> publish→consume pattern with its listener.)

---

## 2. The two Bounded Contexts

Each context is a **top-level package** — the package boundary *is* the context boundary.

```
com.keroles.ddd
├── sharedkernel/        Shared Kernel — tiny, stable, used by BOTH contexts
│   ├── PersonName       value object (non-blank, >= 2 chars) — Teacher and Student both use it
│   └── DomainException  base business exception
│
├── catalog/             SUPPORTING CONTEXT — "a course you author and publish"
│   ├── domain/          Course, CourseStatus, Teacher, Money (value object, catalog-only),
│   │                    repositories (interfaces), CoursePublishedEvent
│   ├── application/     CatalogService, dto/
│   ├── infrastructure/  Spring Data repos + JPA adapters
│   └── interfaces/      CatalogController  (/catalog/...)
│
├── enrollment/          CORE CONTEXT — "a course you join and learn"
│   ├── domain/          Student, Enrollment, Review (3 aggregate roots),
│   │                    Rating, Progress (value objects), EnrollmentStatus,
│   │                    repositories (interfaces),
│   │                    CourseCatalog            (PORT to the other context),
│   │                    EnrollmentPolicy         (DOMAIN SERVICE),
│   │                    ReviewableCourseSpecification (SPECIFICATION),
│   │                    StudentEnrolledEvent
│   ├── application/     EnrollmentService, dto/, eventlistener/
│   ├── infrastructure/  Spring Data repos + JPA adapters,
│   │                    CatalogCourseAdapter     (ANTI-CORRUPTION LAYER)
│   └── interfaces/      EnrollmentController  (/enrollment/...)
│
└── web/                 GlobalExceptionHandler — maps DomainException -> HTTP 400
```

---

## 3. The heart of the lesson: "Course" means two different things

This is the single most important thing in the project. The word **Course** appears in both
contexts and is a **different model in each** — and that is correct (`ddd.md` §3.2).

| | `catalog.domain.Course` | (the course, as seen by) `enrollment` |
|---|---|---|
| It is… | a thing a teacher **authors and publishes** | a thing a student **joins and learns** |
| Lifecycle | `DRAFT → PUBLISHED` | referenced by `Enrollment` (`ACTIVE → COMPLETED`) |
| Attributes it cares about | title, description, price, teacher, status | only: *does it exist?* and *is it published?* |
| Represented by | the `Course` entity (a full aggregate root) | **just an ID** + the tiny `CourseCatalog` port |

The enrollment context never sees `title` or `price` — it doesn't need them. It only asks the two
questions it cares about, through a port it defines in **its own** language:

```
// enrollment/domain/CourseCatalog.java   (the enrollment context's vocabulary)
boolean exists(Long courseId);
boolean isPublished(Long courseId);
```

Forcing one shared `Course` class to serve both jobs is exactly what produces the god object. Two
small models, each correct in its own context, beat one big model that's wrong in both.

---

## 4. The Context Map: Customer/Supplier + Anti-Corruption Layer

```
   ┌────────────────┐    publishes courses     ┌──────────────────────────┐
   │  CATALOG        │  ───────────────────────▶│  ENROLLMENT (core)        │
   │  (supplier,     │                          │  (customer, downstream)   │
   │   upstream)     │                          │                           │
   └────────────────┘                          │  domain depends only on   │
            ▲                                   │  its own CourseCatalog port│
            │ reads via                         └──────────────┬────────────┘
            │ catalog's CourseRepository                       │ implements
            │                                                  ▼
            └──────────────  CatalogCourseAdapter  ◀───  (ANTI-CORRUPTION LAYER,
                             in enrollment/infrastructure      in infrastructure only)
```

- **Direction:** `enrollment` is **downstream** of `catalog` (Customer/Supplier). `catalog` knows
  nothing about `enrollment`.
- **Anti-Corruption Layer:** `enrollment.domain` never imports a single `catalog` class. The only
  place the two contexts touch is `CatalogCourseAdapter` (in `enrollment.infrastructure`), which
  implements the `CourseCatalog` port by reading catalog's repository and translating to the two
  booleans the core cares about. Catalog's model cannot leak into the core.

This isolation is verifiable:

```
enrollment/domain      imports catalog → NONE
enrollment/application imports catalog → NONE
enrollment/* imports catalog → ONLY CatalogCourseAdapter.java   (the ACL seam, by design)
catalog/* imports enrollment → NONE                              (supplier ignores customer)
```

---

## 5. Aggregates and their invariants

Each aggregate is a **small consistency boundary** (`ddd.md` §4.3). Cross-aggregate links are by
**ID only**.

| Aggregate root | Context | Holds | Invariants it guards |
|---|---|---|---|
| `Course` | catalog | title, description, `Money` price, teacherId, `CourseStatus` | title required; price required; must have a teacher; can't publish twice |
| `Teacher` | catalog | `PersonName` | name valid (via VO) |
| `Student` | enrollment | `PersonName` | name valid (via VO) |
| `Enrollment` | enrollment | studentId, courseId, `EnrollmentStatus`, `Progress` | needs a student and a course; progress never moves backwards; reaching 100% completes it; a completed enrollment can't change progress |
| `Review` | enrollment | studentId, courseId, `Rating`, comment | needs student, course, and a rating (1–5 via VO) |

**Value Objects:** `Money` (≥ 0), `PersonName` (non-blank, ≥ 2 chars), `Rating` (1–5), `Progress`
(0–100, monotonic). Each is immutable and self-validating — an invalid one can't be constructed.

**Where each rule lives** (the placement table from `ddd.md` §5, applied):
- "rating is 1–5", "money ≥ 0" → **Value Object** (`Rating`, `Money`).
- "progress can't go backwards", "completed enrollment is frozen" → **Aggregate Root** (`Enrollment`).
- "course must be published **and** not already enrolled to enroll" → **Domain Service**
  (`EnrollmentPolicy`) — it spans the catalog state and existing enrollments, so it belongs to
  neither single entity.
- "only an enrolled, not-yet-reviewed student may review" → **Specification**
  (`ReviewableCourseSpecification`).
- "load, act, save, publish, map" → **Application Service** (`EnrollmentService`, `CatalogService`).

---

## 6. A request, end to end

`POST /enrollment/enrollments  {studentId, courseId}`:

```
EnrollmentController.enroll                       (interfaces — no logic, just delegates)
   └─ EnrollmentService.enroll                    (application — the use case / transaction)
        ├─ studentRepository.findById             confirm the student exists
        ├─ EnrollmentPolicy.enroll                (DOMAIN SERVICE — the business decision)
        │     ├─ courseCatalog.exists / isPublished   asks the other context via the PORT…
        │     │        └─ CatalogCourseAdapter        …answered by the ACL, reading catalog
        │     ├─ enrollmentRepository.exists...        no duplicate enrollment
        │     └─ Enrollment.open(studentId, courseId)  factory builds a valid ACTIVE enrollment
        ├─ enrollmentRepository.save
        ├─ publish StudentEnrolledEvent           (past-tense domain event)
        └─ map to EnrollmentResponse (DTO)        domain objects never leave the boundary
                                                  │
   StudentEnrolledEventListener  ◀───────────────┘  reacts AFTER_COMMIT, decoupled
```

`POST /enrollment/reviews` follows the same shape but gates on the **Specification**:
`ReviewableCourseSpecification.isSatisfiedBy(studentId, courseId)` must hold (enrolled, and not
already reviewed) before `Review.write(...)` is allowed.

---

## 7. What was weak in the previous version — and how this fixes it

The earlier version of this example (a single flat `ddd` package) was tactically competent but
strategically empty. The problems, and the fix here:

### Problem 1 — The god aggregate (the root cause)
`Course` owned `Enrollment` **and** `Review` **and** `Payment` as child collections. That is one
root straddling **three unrelated lifecycles** (a course being authored, a student's progress, a
review being written) — a textbook violation of "keep aggregates small."

> **Fix:** split by lifecycle and context. `Course` lives in **catalog** and owns only its own
> authoring state. `Enrollment` and `Review` are **their own aggregate roots** in **enrollment**,
> linked to a course by **ID**. Each aggregate is now one small consistency boundary.

### Problem 2 — The "OutOfMemory" fix was treating a symptom
Because `Course` owned a possibly-huge `enrollments` collection, the old code did
`enrollments.stream().anyMatch(...)` to check for a duplicate — which lazy-loads every enrollment
row into memory. The old version "fixed" this by moving the check into the application service as a
`SELECT EXISTS` query. That works, but it only exists *because the aggregate was too big.*

> **Fix:** with small aggregates, no root ever holds a giant child collection, so the trap is
> structurally gone. The "is already enrolled?" check is an `existsByStudentIdAndCourseId` query
> living behind the `EnrollmentRepository` interface, used by the **domain service** — by design,
> not as a rescue from a modeling mistake.

### Problem 3 — No strategic design at all
The whole thing was one bounded context, so the word "Course" had to mean everything to everyone,
and the deepest DDD ideas (bounded context, subdomains, context map, ubiquitous-language-per-
context) were absent — exactly the half of Evans' book that delivers the payoff.

> **Fix:** two bounded contexts with one explicit context-map link, and the "Course means two
> things" contrast (§3) made concrete.

### Problem 4 — Missing tactical building blocks
No Domain Service and no Specification — so cross-aggregate rules had nowhere natural to live and
ended up as loose `if` statements in a service.

> **Fix:** `EnrollmentPolicy` (Domain Service) and `ReviewableCourseSpecification` (Specification)
> give those rules a named, testable home in the domain.

### Bonus: the domain is provably framework-independent
`EnrollmentDomainTest` constructs `EnrollmentPolicy`, `Rating`, and `Progress` and asserts their
rules with **plain JUnit — no Spring, no database**. That is only possible because the domain
depends on interfaces it owns, never on infrastructure.

---

## 8. Run it

```bash
cd ddd
./gradlew bootRun        # starts on http://localhost:8080, H2 console at /h2-console
```

A flow that exercises every guard:

```bash
B=http://localhost:8080; H='Content-Type: application/json'

curl -XPOST $B/catalog/teachers -H "$H" -d '{"name":"Alan Kay"}'
curl -XPOST $B/catalog/courses  -H "$H" -d '{"title":"OOP 101","description":"basics","price":49.99,"teacherId":1}'
curl -XPOST $B/enrollment/students -H "$H" -d '{"name":"Grace Hopper"}'

# enroll in a DRAFT course -> rejected by the context-map check
curl -XPOST $B/enrollment/enrollments -H "$H" -d '{"studentId":1,"courseId":1}'

curl -XPOST $B/catalog/courses/1/publish
curl -XPOST $B/enrollment/enrollments -H "$H" -d '{"studentId":1,"courseId":1}'   # now OK
curl -XPOST $B/enrollment/enrollments -H "$H" -d '{"studentId":1,"courseId":1}'   # duplicate -> rejected

# review requires enrollment (Specification); progress is monotonic and auto-completes at 100
curl -XPOST $B/enrollment/reviews -H "$H" -d '{"studentId":1,"courseId":1,"rating":5,"comment":"loved it"}'
curl -XPOST $B/enrollment/enrollments/1/progress -H "$H" -d '{"percent":100}'
curl $B/enrollment/courses/1/reviews
```

```bash
./gradlew test           # runs the framework-free domain test
```
