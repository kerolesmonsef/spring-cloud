# Kafka Mastery TODO — `Kafka`

Goal: go from **beginner** to someone who can whiteboard Kafka internals, build an exactly-once pipeline in Java, and keep a cluster alive through failures.

How to use this file:
- Work **top to bottom**. Each week ends with a **✅ Checkpoint** — don't move on until you can answer it without notes.
- This is the **only progress tracker**. `workflows/learning_roadmap.md` explains *what* and *why*; this file is *what to do next*.
- Java code → `src/main/java/com/keroles/kafka/weekNN/` · shell commands & output → `labs/week_NN/` · your notes → `notes/week_NN.md`.
- Record numbers (throughput, latency, lag, recovery time) in the [Results log](#results-log) at the bottom.
- Budget: ~5–8 h/week → ~12 weeks.

Handy alias (saves typing — the CLI tools are not on `PATH` inside the container):

```bash
kbin() { docker exec -it kafka /opt/kafka/bin/"$@"; }      # single node
# e.g. kbin kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

## Phase 0 — Setup (½ day)

- [x] Copy the learning material into this module (`AGENTS.md`, `workflows/`, `tools/`, `labs/`, `notes/`)
- [x] Init Gradle module (Spring Boot 4.1.1, Java 17, `kafka-clients`), port **8096** registered in `../AGENTS.md`
- [x] Pin broker image to `apache/kafka:4.3.1`, fix single-node `KAFKA_LOG_DIRS` so the volume is really used
- [x] Smoke-test both compose files (done 2026-11-14 — see `AGENTS.md` → Verified facts)
- [ ] Run `./gradlew build` yourself → green
- [ ] `docker compose -f tools/docker-compose.single.yml up -d` → `docker logs kafka | grep "Kafka Server started"`
- [ ] Add the `kbin` alias above to your shell
- [ ] Bookmark: [Kafka docs – Design](https://kafka.apache.org/documentation/#design) and [Implementation](https://kafka.apache.org/documentation/#implementation)
- [ ] Get *Kafka: The Definitive Guide* (2nd ed.) — chapters are referenced below

✅ **Checkpoint:** `kbin kafka-broker-api-versions.sh --bootstrap-server localhost:9092` prints the broker's supported APIs.

---

## Phase 1 — Foundations

### Week 1 — Foundations & CLI
**Learn**
- [ ] Event streaming vs a queue (RabbitMQ): Kafka keeps messages after they are read; consumers track their own position
- [ ] Vocabulary — write a one-line definition of each in `notes/week_01.md`: broker, topic, partition, offset, record (key / value / headers / timestamp), consumer group, retention
- [ ] The **commit log** idea: append-only, ordered, immutable
- [ ] KRaft vs ZooKeeper — why Kafka 4.x removed ZooKeeper (KIP-500)

**Lab** (`labs/week_01/`)
- [ ] `kafka-topics.sh --create --topic orders --partitions 3` then `--describe`
- [ ] Console producer with keys: `--reader-property parse.key=true --reader-property key.separator=:`
- [ ] Console consumer showing partitions: `--from-beginning --formatter-property print.key=true --formatter-property print.partition=true --formatter-property print.offset=true`
- [ ] Prove: the same key **always** lands in the same partition
- [ ] Produce without keys — how are records spread now? (look up the "sticky partitioner")
- [ ] Two consumers, **different** groups → both get everything. Two consumers, **same** group → they split the partitions
- [ ] `docker exec kafka ls /var/lib/kafka/data/orders-0` — first look at segment files (you'll dissect them in week 4)

✅ **Checkpoint:** Draw topic → partitions → offsets. Explain why ordering is guaranteed **only inside a partition**.

### Week 2 — Producers & Consumers in Java
**Learn**
- [ ] Producer pipeline: serializer → partitioner → record accumulator (batches) → sender thread → broker
- [ ] `acks` (0 / 1 / all), `linger.ms`, `batch.size`, `retries`, `delivery.timeout.ms`, `compression.type`
- [ ] Consumer poll loop, `auto.offset.reset` (earliest / latest), `enable.auto.commit`, `commitSync` vs `commitAsync`

**Lab** (package `com.keroles.kafka.week02`)
- [ ] `SimpleProducer` — sends 10 keyed records, prints partition + offset from the `RecordMetadata` callback
- [ ] `SimpleConsumer` — poll loop with clean shutdown (`consumer.wakeup()` from a shutdown hook)
- [ ] Switch to manual commits; kill the consumer before commit → see the re-delivery
- [ ] `CustomPartitioner` — e.g. key `"VIP-*"` always to partition 0
- [ ] Throughput test: 100k records with `acks=0/1/all` × `linger.ms=0/20` → fill the Results log
- [ ] Compare with the built-in `kafka-producer-perf-test.sh`

✅ **Checkpoint:** What is a producer batch, and when is it sent? With `acks=all`, what does "all" actually mean?

---

## Phase 2 — Internals ⭐ (your main focus)

### Week 3 — Partitions, Replication, ISR (3-node cluster)
- [ ] `docker compose -f tools/docker-compose.single.yml down` → `docker compose -f tools/docker-compose.cluster.yml up -d`
- [ ] Learn: leader / follower, replication factor, **ISR**, `min.insync.replicas`, **high watermark**, leader epoch, unclean leader election, ELR (eligible leader replicas, new in 4.x)
- [ ] Learn: the KRaft controller quorum (Raft) — `kafka-metadata-quorum.sh describe --status`
- [ ] Create topic RF=3, `min.insync.replicas=2`; `--describe` and read every column
- [ ] `docker stop kafka-2` → watch leaders move and ISR shrink; `docker start kafka-2` → watch it catch up
- [ ] Produce with `acks=all` while 1 broker is down (works) → then 2 down (fails: `NotEnoughReplicas`)
- [ ] Remember: inside a container use `kafka-1:9092`; `localhost:19092` only works from your host

✅ **Checkpoint:** Describe a data-loss scenario step by step, and the exact settings (`acks`, RF, `min.insync.replicas`, `unclean.leader.election.enable`) that prevent it.

### Week 4 — Storage internals ⭐
- [ ] Back to single node. Learn: partition directory, `.log` segment, `.index` (offset index), `.timeindex`, `.snapshot`, `leader-epoch-checkpoint`
- [ ] Learn: sparse index + binary search, `segment.bytes`, `segment.ms`, retention by time/size, page cache, zero-copy (`sendfile`), sequential I/O → why Kafka is fast
- [ ] `kafka-dump-log.sh --files <...>.log --print-data-log` — read the record batches
- [ ] Dump `.index` and `.timeindex` — match index entries to positions in the `.log`
- [ ] Topic with `segment.bytes=1024` → produce → watch segments roll; set short `retention.ms` → watch old segments get deleted
- [ ] **Log compaction**: `cleanup.policy=compact`, produce many versions per key + a tombstone (null value) → watch old values disappear
- [ ] Read the docs section "Log Compaction" fully

✅ **Checkpoint:** A consumer asks for offset 123456. Walk through exactly how the broker finds it (segment file name → index → scan). Why is the index sparse?

### Week 5 — Consumer groups, offsets, rebalancing
- [ ] Learn: group coordinator, `__consumer_offsets`, assignors (range, round-robin, sticky, cooperative-sticky)
- [ ] Learn: the new consumer group protocol (**KIP-848**, `group.protocol=consumer`) vs classic — check in the docs which one your client version uses by default
- [ ] Learn: static membership (`group.instance.id`), `session.timeout.ms`, `max.poll.interval.ms`, lag
- [ ] Run 3 consumers in one group (topic with 6 partitions) → kill one → watch the reassignment in logs
- [ ] Reproduce a rebalance storm: sleep longer than `max.poll.interval.ms` inside the loop
- [ ] `kafka-consumer-groups.sh --describe --group <g>` → read LAG; `--reset-offsets --to-earliest` (dry-run then execute)
- [ ] Read `__consumer_offsets` with the offsets formatter

✅ **Checkpoint:** Why can't you have more *active* consumers than partitions? How do you choose a partition count?

---

## Phase 3 — Reliability & Patterns

### Week 6 — Delivery semantics, idempotence, transactions
- [ ] Learn: at-most-once / at-least-once / exactly-once; idempotent producer (producer ID + sequence numbers)
- [ ] Learn: transactions (`transactional.id`, transaction coordinator, `__transaction_state`, commit markers, `isolation.level=read_committed`), zombie fencing
- [ ] Force duplicates: `enable.idempotence=false` + retries + a broker restart mid-send → count duplicates
- [ ] Turn on idempotence → prove duplicates are gone
- [ ] Transactional consume → transform → produce app; kill it mid-transaction → `read_committed` consumer sees nothing partial
- [ ] Look at the commit markers with `kafka-dump-log.sh`

✅ **Checkpoint:** Is exactly-once end-to-end possible when the consumer writes to an external DB? (Discuss the outbox pattern and idempotent consumers.)

### Week 7 — Schemas & Schema Registry
- [ ] Learn: why schemas; Avro vs Protobuf vs JSON Schema; compatibility modes (BACKWARD / FORWARD / FULL); subject naming strategies
- [ ] Add Schema Registry to the compose file — **pick a free host port and add it to `../AGENTS.md` Used Ports**
- [ ] Produce/consume Avro from Java (add serializer deps to `build.gradle`)
- [ ] Evolve the schema (add a field with a default) → old consumers still work
- [ ] Break compatibility on purpose (remove a required field) → see the registry reject it

✅ **Checkpoint:** List 3 backward-compatible changes and 3 breaking changes.

---

## Phase 4 — Ecosystem

### Week 8 — Kafka Connect & CDC (Debezium)
- [ ] Learn: workers, connectors, tasks, converters, SMTs, source vs sink, distributed mode, offset storage, DLQ
- [ ] Add Postgres + Connect (with Debezium) to compose — **register ports**
- [ ] Postgres → Debezium source → topic → file/JDBC sink
- [ ] INSERT / UPDATE / DELETE rows → read the `before` / `after` envelopes
- [ ] Add an SMT (e.g. `ExtractNewRecordState`) and a dead-letter queue for bad records

✅ **Checkpoint:** Compare CDC with the transactional outbox pattern — when would you pick each?

### Week 9 — Kafka Streams
- [ ] Uncomment `kafka-streams` in `build.gradle`
- [ ] Learn: topology, KStream vs KTable vs GlobalKTable, stateless vs stateful, joins, windows (tumbling / hopping / session), state stores (RocksDB), changelog topics, repartitioning, standby replicas, `exactly_once_v2`, Interactive Queries
- [ ] (a) Word count
- [ ] (b) Enrich orders with a customers KTable (join)
- [ ] (c) 5-minute windowed aggregation
- [ ] Kill the app → restart → watch state restore from the changelog topic
- [ ] Unit-test a topology with `TopologyTestDriver`

✅ **Checkpoint:** Why does a stream–table join require co-partitioning?

### Week 10 — ksqlDB + event-driven patterns
- [ ] Add ksqlDB to compose — **register port**. Learn streams/tables, push vs pull queries
- [ ] Re-do the week-9 aggregation in ksqlDB
- [ ] Learn: event sourcing, CQRS, outbox, saga (orchestration vs choreography), retry topics, dead-letter topics
- [ ] Uncomment `spring-kafka`; build an order → payment → shipping **choreographed saga** with `@KafkaListener` + `KafkaTemplate`
- [ ] Add retry topics + DLT (`@RetryableTopic`) and send a poison-pill message

✅ **Checkpoint:** When do you choose Streams vs ksqlDB vs plain consumers?

---

## Phase 5 — Production & Mastery

### Week 11 — Production
- [ ] Capacity planning: partitions, disk, network
- [ ] Key configs for throughput vs latency (broker / producer / consumer)
- [ ] Monitoring: JMX → Prometheus → Grafana — **register ports**. Watch: under-replicated partitions, request latency, ISR shrink rate, consumer lag
- [ ] Security: TLS + SASL/SCRAM on the cluster, then ACLs (deny then allow a user)
- [ ] Quotas: throttle a noisy producer
- [ ] `kafka-reassign-partitions.sh` — move a partition to another broker
- [ ] `kafka-producer-perf-test.sh` / `kafka-consumer-perf-test.sh` → tune → log results
- [ ] Read about: MirrorMaker 2, tiered storage (KIP-405), rolling upgrades

✅ **Checkpoint:** You're paged: "under-replicated partitions > 0". Write the runbook in `notes/week_11.md`.

### Week 12 — Capstone + failure drills
- [ ] Pick a capstone:
  - [ ] (1) Real-time fraud alerts: Postgres → Debezium → Kafka → Streams → sink + dashboard, **or**
  - [ ] (2) Event-sourced wallet/ledger with exactly-once transfers (pairs with `../EWalletDDD`)
- [ ] Drill: kill a broker
- [ ] Drill: kill the active controller
- [ ] Drill: fill a broker's disk
- [ ] Drill: consumer lag explosion
- [ ] Drill: poison-pill message
- [ ] Drill: schema break
- [ ] Drill: network partition (`docker network disconnect`)

✅ **Master test** — pass all without notes:
1. [ ] Whiteboard a write: producer → leader → followers → high watermark → ack
2. [ ] What's inside a partition directory, and how does an offset lookup work?
3. [ ] Choose and defend settings for "no data loss" and for "max throughput"
4. [ ] Explain a rebalance and how to avoid storms
5. [ ] Explain how exactly-once works and where it stops
6. [ ] Debug a lagging consumer from metrics alone

---

## Results log

| Date | Week | Experiment | Setting(s) | Result |
|------|------|------------|------------|--------|
|      |      |            |            |        |
