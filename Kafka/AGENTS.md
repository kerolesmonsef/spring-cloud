# Learning Kafka – AGENTS.md

> This project follows the **WAT Framework**: **W**orkflows · **A**gent · **T**ools
> It is a **technical learning project**, not a marketing project.

## Goal
Go from Kafka beginner to master: how Kafka works inside (topics, partitions, log segments, indexes, replication), how to build with it (Java), advanced features (transactions, Streams, Connect), and how to run it in production.

## Learner profile (gathered 2026-11-12)
| Item | Answer |
|---|---|
| Name | Keroles Monsef |
| Kafka level | Self-rated **Beginner** (the Mifos project notes Kafka among the stack, so go step by step and skip nothing) |
| Language | **Java** (17, Spring Boot background) |
| Pace | Hands-on, **5–8 hours/week** → ~12 weeks |
| Focus areas | Internals · Streams/ksqlDB/Connect · Ops & Production · Patterns (exactly-once, transactions, schema registry, event sourcing, CQRS) |
| Teaching language | English in files; Egyptian Arabic is fine in chat |

## Available Workflows
- `workflows/learning_roadmap.md` — 12-week roadmap, each week has concepts, labs, a checkpoint and a "master test"
- `workflows/study_session.md` — how a single study session runs (explain → lab → quiz → notes)

## Tools / Lab environment
- `tools/docker-compose.single.yml` — 1 broker (KRaft), for weeks 1–2
- `tools/docker-compose.cluster.yml` — 3 brokers (KRaft), for replication/failure labs from week 3
- Put code in `labs/week_NN_*/`, personal notes in `notes/`.
- Compose files are written from docs and have not been run in the cloud session, so test them locally first and fix them if they fail.

## Tool Usage
- For internet searches use `websearch`, `webfetch`, AND **browsermcp** browser tools together. Mix results from all sources for the most comprehensive research. Prefer all available search tools in parallel.
- Kafka changes between versions (4.x is KRaft-only, no ZooKeeper). Check the official docs before stating version-specific facts.

## Rules
- Teach with experiments: every concept gets a lab where the learner can see it (e.g. `kafka-dump-log` on a segment file).
- Do not move to the next week until the checkpoint is passed.
- Never reuse tools/workflows from other projects (only `tools/send_telegram.py` is shared).
- Summarize what was done after each task.
