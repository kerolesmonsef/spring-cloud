# Kafka – AGENTS.md

> Module-level guide. The repo-wide rules in `../AGENTS.md` still apply
> (learner-first explanations, no commits, properties not YAML, Used Ports table).

## Goal
Go from Kafka beginner to master: how Kafka works inside (topics, partitions, log segments, indexes, replication), how to build with it (Java), advanced features (transactions, Streams, Connect), and how to run it in production.

## Learner profile (gathered 2026-11-12)
| Item | Answer |
|---|---|
| Kafka level | Self-rated **Beginner** — go step by step and skip nothing |
| Language | **Java** (17, Spring Boot background) |
| Pace | Hands-on, **5–8 hours/week** → ~12 weeks |
| Focus areas | Internals · Streams/ksqlDB/Connect · Ops & Production · Patterns (exactly-once, transactions, schema registry, event sourcing, CQRS) |
| Teaching language | English in files; Egyptian Arabic is fine in chat |

## Where things live
| Path | What |
|---|---|
| `TODO.md` | **The single progress tracker.** Step-by-step checklist for all 12 weeks + results log. Tick boxes here. |
| `workflows/learning_roadmap.md` | Reference: what each week covers and why (concepts, lab idea, checkpoint, master test). No progress tracking here. |
| `workflows/study_session.md` | How one study session runs (explain → lab → quiz → notes). |
| `tools/docker-compose.single.yml` | 1 broker (KRaft), weeks 1–2, 4. Host port `9092`. Data on a named volume at `/var/lib/kafka/data`. |
| `tools/docker-compose.cluster.yml` | 3 brokers (KRaft), weeks 3+. Host ports `19092`, `29092`, `39092`. No volumes (throwaway). |
| `src/main/java/com/keroles/kafka/weekNN/` | Java code for week NN (one package per week). |
| `labs/week_NN/` | Shell commands, lab write-ups, sample output for week NN. |
| `notes/week_NN.md` | Personal notes: what I learned, what confused me. |

## Build / run
- Single Gradle module (Spring Boot 4.1.1, Java 17), not Maven. Run everything from this directory.
- `./gradlew build` — compiles and runs the context smoke test (does **not** need a broker).
- `./gradlew bootRun` — starts the app (port `8096`, registered in `../AGENTS.md`).
- Run a single lab class from the IDE (each lab class has its own `main`), or via a Gradle task you add for that week.
- Kafka client: `org.apache.kafka:kafka-clients`, version managed by the Spring Boot BOM (4.2.1 at init). Broker image is pinned to `apache/kafka:4.3.1`. Newer brokers accept older clients, so this mismatch is fine.
- Spring Kafka and Kafka Streams are commented out in `build.gradle` — enable them when the roadmap reaches them, so weeks 2–6 use the raw client API.

## Verified facts (tested 2026-11-14 on `apache/kafka:4.3.1`)
- Both compose files start and work. Single node: topic create / keyed produce / consume OK. Cluster: RF=3 topic, stopping `kafka-2` moves leaders and shrinks ISR as expected.
- The image's default `log.dirs` is `/tmp/kraft-combined-logs`; the single-node compose overrides it with `KAFKA_LOG_DIRS=/var/lib/kafka/data` so the volume is really used. Segment files appear under `/var/lib/kafka/data/<topic>-<partition>/`.
- CLI tools live at `/opt/kafka/bin/` inside the container (not on `PATH`).
- Kafka 4.x console tools use `--reader-property` (producer) and `--formatter-property` (consumer) instead of the old `--property`.
- Inside a cluster container, use the internal listener `kafka-N:9092`. `localhost:19092` etc. only work from the host.

## Tool usage
- Kafka changes between versions (4.x is KRaft-only, no ZooKeeper). Check the official docs for the pinned version before stating version-specific facts.

## Rules
- Teach with experiments: every concept gets a lab where the learner can see it (e.g. `kafka-dump-log.sh` on a segment file).
- Do not move to the next week until the checkpoint is passed.
- New host ports needed later (Schema Registry, Connect, ksqlDB, Prometheus, Grafana, Postgres…) must be checked against and added to the Used Ports table in `../AGENTS.md`.
- Summarize what was done after each task.
