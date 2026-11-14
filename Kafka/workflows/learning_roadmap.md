# Kafka Master Roadmap (12 weeks, 5–8 h/week, Java)

**Rule:** each week = ~1.5 h concepts + ~4 h labs + ~1 h checkpoint/notes. Do not skip the checkpoint.
**Final result:** you can explain Kafka internals on a whiteboard, build and tune an exactly-once pipeline, and run a cluster through failures.

## Progress
Progress is tracked in **`../TODO.md`** (single tracker). This file is the reference for *what* each week covers and *why*.

---

## Phase 1 — Foundations (Weeks 1–2)

### Week 1: Foundations & CLI
- **Concepts:** event streaming vs queues (RabbitMQ), broker, topic, partition, offset, record (key/value/headers/timestamp), the commit log idea, retention. KRaft vs old ZooKeeper.
- **Lab:** run `tools/docker-compose.single.yml`. Use `kafka-topics.sh`, `kafka-console-producer.sh`, `kafka-console-consumer.sh`. Create a topic with 3 partitions, produce keyed messages, and see that the same key always lands in the same partition (`--formatter-property print.partition=true`; Kafka 4.x renamed `--property`).
- **Checkpoint:** explain why ordering is guaranteed only inside a partition. Draw topic → partitions → offsets.

### Week 2: Producers & Consumers (Java)
- **Concepts:** `KafkaProducer` flow (serializer → partitioner → batching → send), `acks`, `linger.ms`, `batch.size`, `retries`, `compression.type`; `KafkaConsumer` poll loop, `auto.offset.reset`, commits.
- **Lab:** Java classes in package `com.keroles.kafka.week02` of this Gradle module. Write a producer and a consumer, then a custom partitioner. Measure throughput with `acks=0/1/all` and different `linger.ms`.
- **Checkpoint:** what happens when `acks=all` and a broker is down? What is a producer batch?

## Phase 2 — Internals (Weeks 3–5)  *(your main focus)*

### Week 3: Partitions, Replication, ISR
- **Concepts:** leader/follower, replication factor, ISR, `min.insync.replicas`, high watermark, leader epoch, unclean leader election, controller quorum (KRaft/Raft), rack awareness.
- **Lab:** run `docker-compose.cluster.yml`. Create a topic with RF=3, run `kafka-topics.sh --describe`, `docker stop` the leader, and watch the new leader and ISR shrink. Produce with `acks=all` and `min.insync.replicas=2` while 1 then 2 brokers are down.
- **Checkpoint:** explain a data-loss scenario and the exact settings that prevent it.

### Week 4: Storage Internals ⭐
- **Concepts:** the partition directory, **log segments** (`.log`), **offset index** (`.index`), **time index** (`.timeindex`), `.snapshot`, `leader-epoch-checkpoint`, sparse index and binary search, `segment.bytes`, `log.roll`, retention by time/size, **log compaction** (tombstones, `min.cleanable.dirty.ratio`), page cache and zero-copy (`sendfile`), sequential I/O, and why Kafka is fast.
- **Lab:** find the data directory in the container. Run `kafka-dump-log.sh --files <segment> --print-data-log`, and the same on `.index` and `.timeindex`. Set a tiny `segment.bytes`, produce, and watch the segments roll and get deleted. Build a compacted topic and observe old keys disappearing.
- **Checkpoint:** a consumer asks for offset 123456. Walk through exactly how the broker finds it (segment file name → index → scan). Why is the index sparse?

### Week 5: Consumer Groups, Offsets, Rebalancing
- **Concepts:** group coordinator, `__consumer_offsets`, partition assignment (range, round-robin, sticky, cooperative-sticky), the new consumer group protocol (KIP-848), static membership, `session.timeout.ms`, `max.poll.interval.ms`, lag.
- **Lab:** run 3 consumers in a group and kill one to see the rebalance. Reproduce a rebalance storm with a slow `poll`. Read `__consumer_offsets` with the formatter. Monitor lag with `kafka-consumer-groups.sh`.
- **Checkpoint:** why can you not have more active consumers than partitions? How do you choose the partition count?

## Phase 3 — Reliability & Patterns (Weeks 6–7)

### Week 6: Delivery Semantics, Idempotence, Transactions
- **Concepts:** at-most/at-least/exactly-once, idempotent producer (PID + sequence numbers), transactions (`transactional.id`, coordinator, `__transaction_state`, markers, `isolation.level=read_committed`), the consume-transform-produce loop, zombie fencing.
- **Lab:** produce duplicates by forcing retries without idempotence. Enable `enable.idempotence=true` and prove the duplicates are gone. Build a transactional consume-transform-produce app and kill it mid-transaction.
- **Checkpoint:** is exactly-once end-to-end possible with an external DB? (Answer: discuss the outbox pattern and idempotent consumers.)

### Week 7: Schemas
- **Concepts:** why schemas, Avro vs Protobuf vs JSON Schema, Schema Registry, compatibility modes (BACKWARD/FORWARD/FULL), subject naming strategies, schema evolution.
- **Lab:** add Schema Registry to the compose file. Produce Avro from Java, evolve the schema (add field with default), then break compatibility on purpose and see the rejection.
- **Checkpoint:** which changes are backward-compatible, and which are breaking?

## Phase 4 — Ecosystem (Weeks 8–10)

### Week 8: Kafka Connect & CDC
- **Concepts:** workers, connectors, tasks, converters, SMTs, source vs sink, distributed mode, offsets storage, dead letter queue. CDC with Debezium (WAL / binlog).
- **Lab:** PostgreSQL → Debezium source → Kafka → JDBC/Elasticsearch/file sink. Watch the insert/update/delete events (`before`/`after`).
- **Checkpoint:** compare CDC with the outbox pattern.

### Week 9: Kafka Streams
- **Concepts:** topology, KStream vs KTable vs GlobalKTable, stateless and stateful ops, joins, windowing (tumbling, hopping, session), state stores (RocksDB), changelog topics, repartitioning, standby replicas, `processing.guarantee=exactly_once_v2`, Interactive Queries.
- **Lab:** (a) word count, (b) enrich orders with a customer KTable join, (c) a 5-minute windowed aggregation. Kill the app and watch the state restore.
- **Checkpoint:** why does a join require co-partitioning?

### Week 10: ksqlDB + Event-driven Patterns
- **Concepts:** ksqlDB streams/tables/push vs pull queries; event sourcing, CQRS, outbox, saga (orchestration vs choreography), dead-letter topics, retry topics, the schema-per-event design.
- **Lab:** the same aggregation as in Week 9, now in ksqlDB. Then build a small order → payment → shipping saga with Kafka and Spring Boot.
- **Checkpoint:** when do you choose Streams over ksqlDB, and plain consumers over both?

## Phase 5 — Production & Mastery (Weeks 11–12)

### Week 11: Production
- **Concepts:** capacity planning (partitions, disk, network), key broker/producer/consumer configs for throughput vs latency, **monitoring** (JMX → Prometheus → Grafana: under-replicated partitions, request latency, ISR shrink rate, consumer lag), security (TLS, SASL/SCRAM, ACLs), quotas, MirrorMaker 2 / cluster linking, tiered storage, rolling upgrades, partition reassignment, KRaft operations.
- **Lab:** add Prometheus + Grafana. Enable SASL/SCRAM + TLS on the cluster. Add ACLs. Run `kafka-reassign-partitions.sh`. Run `kafka-producer-perf-test.sh` and tune.
- **Checkpoint:** you are paged for "under-replicated partitions > 0". Write the runbook.

### Week 12: Capstone + Failure Drills
- **Capstone (pick one):** (1) real-time fraud/alerts pipeline: Postgres → Debezium → Kafka → Streams → sink + dashboard; (2) event-sourced wallet/ledger with exactly-once transfers (fits your fintech work).
- **Drills:** kill a broker, kill the controller, fill a disk, lag explosion, poison pill message, schema break, network partition (use `tc` or `docker network disconnect`).
- **Master test (you pass if you can do all of these without notes):**
  1. Whiteboard a write: producer → leader → followers → high watermark → ack.
  2. Explain what is inside a partition directory and how an offset lookup works.
  3. Choose and defend settings for "no data loss" and for "max throughput".
  4. Explain a rebalance and how to avoid storms.
  5. Explain how exactly-once works and where it stops.
  6. Debug a lagging consumer from metrics alone.

## Resources
- Official docs: kafka.apache.org/documentation (read the Design and Implementation sections in full)
- Book: *Kafka: The Definitive Guide* (2nd ed.)
- Book: *Designing Event-Driven Systems* (Ben Stopford, free ebook)
- Book: *Kafka Streams in Action* (2nd ed.)
- KIPs: KIP-500 (KRaft), KIP-98 (transactions), KIP-848 (new consumer protocol), KIP-405 (tiered storage)
- Confluent Developer courses (free) and Confluent blog
