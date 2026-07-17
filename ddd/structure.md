```
com.ewallet.cashout/
│
├── api/                                  ← INBOUND ADAPTERS (the "left" edge / driving side)
│   ├── CashoutController.java            // REST controller. THIN. Maps HTTP <-> app service. NO logic.
│   ├── RailWebhookController.java        // Receives async LuLu/Mbank callbacks -> app service.
│   └── dto/
│       ├── CashoutRequestDto.java        // Inbound JSON shape + @Valid annotations. NOT a domain object.
│       └── CashoutResponseDto.java       // Outbound JSON shape. Keeps domain out of the wire.
│
├── application/                          ← ORCHESTRATION. Use cases. NO business rules.
│   ├── CashoutAppService.java            // @Transactional. THE PUBLIC DOOR (Step: only this is exposed).
│   │                                     //   load aggregate -> call domain -> save -> publish events.
│   ├── command/
│   │   ├── RequestCashoutCommand.java    // Immutable input to a use case (id, amount, railType).
│   │   └── ConfirmCashoutCommand.java
│   ├── port/                             ← OUTBOUND PORTS (interfaces THIS context needs from outside).
│   │   ├── PayoutRailPort.java           //   "dispatch a payout" — in OUR language. 1 port, N adapters.
│   │   └── LedgerReservationPort.java    //   "reserve / settle / release funds" — in OUR language.
│   ├── listener/
│   │   └── RailCallbackListener.java     // Handles async rail results -> calls app service.
│   └── saga/
│       └── CashoutSaga.java              // PROCESS MANAGER: reserve -> dispatch -> settle/compensate.
│                                         //   Lives here because it orchestrates, not "business rule".
│
├── domain/                               ← THE HEART. Pure Java. No Spring, no JPA, no HTTP.
│   ├── model/
│   │   ├── CashoutRequest.java           // AGGREGATE ROOT. Guards state transitions + invariants.
│   │   ├── CashoutId.java                // VALUE OBJECT — typed id (not raw Long/String).
│   │   ├── CashoutStatus.java            // enum: REQUESTED,RESERVED,DISPATCHED,CONFIRMED,FAILED,COMPENSATED.
│   │   ├── RailType.java                 // enum: AANI, LULU, MBANK.
│   │   └── Money.java                    // VALUE OBJECT (amount+currency). Shared-kernel candidate.
│   ├── service/
│   │   └── RailSelectionService.java     // DOMAIN SERVICE — logic spanning >1 object (pick rail by rules).
│   ├── specification/
│   │   └── WithinDailyLimitSpec.java     // SPECIFICATION — a named, reusable, testable business rule.
│   ├── event/
│   │   ├── CashoutRequested.java         // DOMAIN EVENTS — past tense, immutable records.
│   │   ├── CashoutConfirmed.java
│   │   └── CashoutFailed.java
│   └── repository/
│       └── CashoutRepository.java        // REPOSITORY PORT (interface). Domain-owned. Collection illusion.
│
└── infrastructure/                       ← OUTBOUND ADAPTERS (the "right" edge / driven side). Framework lives here.
    ├── persistence/
    │   ├── CashoutJpaEntity.java         // DB row shape. @Entity/@Table. NOT the aggregate.
    │   ├── SpringDataCashoutRepo.java    // extends JpaRepository. Raw DB access.
    │   ├── CashoutRepositoryAdapter.java // implements domain CashoutRepository. Maps entity<->aggregate.
    │   └── CashoutMapper.java            // entity <-> aggregate translation.
    ├── rail/                             ← THE ACL LAYER — one adapter per external rail.
    │   ├── AaniRailAdapter.java          // implements PayoutRailPort. Translates our model -> ISO 20022.
    │   ├── LuluRailAdapter.java          // implements PayoutRailPort. Handles async correspondent flow.
    │   ├── MbankRailAdapter.java         // implements PayoutRailPort.
    │   ├── RailAdapterRouter.java        // picks the right adapter by RailType (Strategy pattern).
    │   └── external/                     // QUARANTINE: raw vendor DTOs/clients live ONLY here.
    │       ├── AaniIso20022Message.java  //   ugly external shapes never leak past this folder.
    │       └── LuluRemittanceDto.java
    ├── ledger/
    │   └── LedgerReservationAdapter.java // implements LedgerReservationPort. Calls Ledger's AppService/API.
    ├── messaging/
    │   └── CashoutEventPublisher.java    // Publishes domain events to Kafka/Rabbit (or ApplicationEventPublisher).
    └── config/
    └── CashoutConfig.java            // Spring @Configuration — wires ports to adapters.
```