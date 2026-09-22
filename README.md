# Audit Log Service

Runnable Java 21 / Spring Boot 3 MVP for append-only, tenant-scoped audit events.

## Engineering governance

`ENGINEERING_WORKFLOW.md` defines the normalized problem, dependency-ordered
tasks, acceptance criteria, AI-assisted execution protocol, quality gates,
security controls, risk management, human approval points, and final
engineering summary requirements. `AI_USAGE_LOG.md` records generated, edited,
and rejected AI output with rationale and validation status. `SCENARIO_B.md`
contains the retention, redaction, hash-version, and export design.
`SCENARIO_A.md`, `SCENARIO_B.md`, and `SCENARIO_C.md` contain concise scenario
requirements, scope, and acceptance summaries. `TRADEOFFS_AND_CHALLENGES.md`
records cross-scenario alignment, design trade-offs, engineering challenges,
and release guardrails.
`REQUIREMENTS_HLD_LLD.md` is the dedicated core requirements, HLD, LLD,
functional/non-functional requirements, execution, quality-gate, and oversight
specification for all three scenarios.
`SETUP.md` contains complete local prerequisites, startup, smoke-test,
Scenario B operation, and troubleshooting instructions. `FINAL_ENGINEERING_SUMMARY.md`
records the delivered artifacts, validation approach, risks, trade-offs,
assumptions, and release limitations.

## High-level requirements

### Greenfield system requirements

The first production-ready version must:

* Accept audit events through REST and Kafka without allowing either channel to
  bypass the same validation and persistence rules.
* Store events in an append-only, tenant- and stream-scoped model with
  deterministic sequence numbers, canonical payloads, and a SHA-256 hash chain.
* Make retries safe through idempotency keys and immutable event IDs.
* Provide tenant-scoped read and verification APIs with bounded pagination,
  clear validation errors, and authorization boundaries.
* Detect altered, missing, reordered, or incorrectly linked records during
  verification and identify the first invalid sequence.
* Publish durable external checkpoints or signed manifests before claiming
  independently verifiable tamper evidence.
* Scale horizontally by partitioning work by tenant/stream, keeping Kafka
  availability separate from the authoritative append transaction, and
  exposing health, lag, verification, and checkpoint-age metrics.
* Avoid update and delete operations; corrections must be represented as new
  compensating events.

### Feature-extension requirements

New ingestion channels (gRPC, webhooks, batch files, CDC, SDKs) must implement
the shared internal event-ingestion contract rather than duplicate hashing,
validation, sequencing, or authorization logic. New outbound integrations must
consume committed events through the outbox/Kafka boundary and must tolerate
at-least-once delivery using the immutable event ID.

Schema changes must be versioned and backward-compatible for active producers
and consumers. Changes to canonicalization or hash inputs require a new
algorithm version and must never rewrite historical records. Retention,
archival, replay, and checkpoint features must preserve the ability to verify
the covered historical range.

### Test requirements

The service must maintain:

* Unit tests for canonical JSON ordering, hash calculation, sequence handling,
  idempotency, and verification failure reporting.
* API contract tests for valid requests, malformed payloads, authorization
  boundaries, pagination, duplicate requests, and unsupported mutations.
* PostgreSQL integration tests for migrations, concurrent appends, unique
  constraints, transaction rollback, and restore/verification behavior.
* Kafka contract and failure tests covering retries, duplicate delivery,
  partitioning, ordering assumptions, poison messages, and dead-letter routing.
* Property-based or generated tests proving that any event mutation, deletion,
  insertion, or reordering is detected by verification.
* Load and failure-injection tests for throughput, append latency, consumer
  lag, checkpoint recovery, and database/Kafka interruptions.

### Documentation requirements

Documentation must define the event envelope and versioning policy, REST
OpenAPI contract, Kafka topics and schemas, ordering and delivery guarantees,
idempotency behavior, tenant isolation, verification semantics, retention
policy, key rotation, backup/restore verification, and operational alerts.
Runbooks must explain how to investigate a broken chain without modifying
source records and how to recover integrations safely.

### Ambiguous requirements requiring an explicit decision

The following are intentionally not assumed by the MVP and must be decided
before production commitments:

| Decision | Options and impact |
| --- | --- |
| Ordering scope | Per tenant, per stream, or global. Per-stream ordering scales best; global ordering creates a bottleneck. |
| Delivery guarantee | At-least-once with deduplication, or exactly-once at higher operational and integration cost. |
| Integrity boundary | Local database tamper evidence only, or signed external checkpoints/WORM archive for independent verification. |
| Event timestamp policy | Trust producer time, use server receipt time, or store both and use sequence for ordering. |
| Retention/deletion policy | Indefinite retention, regulated retention windows, or cryptographic tombstone events without physical deletion. |
| Payload limits | Maximum event size, batch size, nesting depth, and permitted content types. |
| Tenant isolation | Shared schema, separate schemas, or separate databases; this affects cost and isolation strength. |
| Authentication and authorization | OIDC/JWT, mTLS, API keys, or a combination; define append/read/verify permissions separately. |
| Availability target | RPO/RTO, regional topology, and whether active-active writes are required. |
| Search requirements | PostgreSQL queries only, indexed JSON fields, or a separate search system that remains non-authoritative. |

## Architecture

* **PostgreSQL is the source of truth.** Flyway creates `audit_events`, whose
  composite primary key is `(tenant_id, stream_id, sequence_number)`.
* Every event stores a canonical JSON payload, a SHA-256 hash, and the previous
  event hash. The hash input also includes tenant, stream, sequence, event ID,
  type, and occurrence time.
* `AuditEventService` owns idempotent appends. The `(tenant, stream,
  idempotency-key)` unique constraint makes retries safe; reusing a key with a
  different payload returns `409 Conflict`.
* `adapter` contains an intentionally no-op `AuditEventPublisher` seam. Kafka
  dependencies are present for the future adapter, but the MVP neither connects
  to Kafka nor claims to publish to it.
* `checkpoint.CheckpointService` is an explicit future boundary. Verification
  currently proves only the locally stored chain, not an externally anchored
  attestation.

There are deliberately no update or delete endpoints.

## Run

For the complete setup sequence, see [`SETUP.md`](SETUP.md).

The default configuration expects PostgreSQL at `localhost:5432`:

```text
DB_URL=jdbc:postgresql://localhost:5432/auditlog
DB_USERNAME=auditlog
DB_PASSWORD=auditlog
```

Then run with a Java 21 JDK and Maven:

```bash
mvn spring-boot:run
```

Flyway runs the schema migration at startup. The repository does not require a
Kafka broker.

## API outline

All endpoints are under `/v1/tenants/{tenantId}`.

* `POST /streams/{streamId}/events` — append one event. Requires the
  `Idempotency-Key` header. Body: `eventType`, `payload`, optional `actorId`,
  `resourceType`, `resourceId`, `timestamp` (`occurredAt` is an alias), and
  `eventId`.
* `POST /streams/{streamId}/events:batch` — append a list; each item includes
  its own `idempotencyKey`.
* `GET /streams/{streamId}/events?from=&to=&limit=` — ordered stream query.
  Queries also accept any combination of `actorId`, `resourceType`,
  `resourceId`, `eventType`, and zero-based `page` (or a direct `offset`).
* `GET /events/{eventId}` — tenant-scoped point lookup.
* `GET /streams/{streamId}/verify` — recompute and check the complete local
  chain.
* `GET /audit/verify?tenantId=&streamId=` — the same full-chain verification
  endpoint; a response identifies the first invalid sequence.
* `GET /audit/export?tenantId=&actorId=` or `resourceId=` — a
  `audit-log-bundle-v1` JSON bundle containing selected events and per-stream
  chain boundary metadata (`previousHash`, terminal hash, and whether the
  selected range is contiguous). A recipient can recompute each included event
  and the links between consecutive included events; a non-contiguous export
  also reports the preceding hash as an external boundary anchor rather than
  pretending the filtered bundle is a complete stream.
* `POST /events/{eventId}/redactions` — appends a compensating
  `audit.redaction` event. It never updates the original row. Paths are
  deterministic dot paths (or array indexes/`*`), and replaced values use the
  literal `[REDACTED]` marker.

The event envelope includes `eventType`, `actorId`, `resourceType`,
`resourceId`, `payload`, and `timestamp` (with `occurredAt` retained as a
backward-compatible alias). A supplied timestamp is preserved; when omitted,
the server assigns the timestamp at append time. All envelope fields are
included in the canonical hash input.

## Retention and archive verification

Retention is intentionally explicit rather than an unreviewed destructive
job. Set `AUDIT_ARCHIVE_AFTER` (for example `365d`) and call
`POST /v1/tenants/{tenantId}/streams/{streamId}/retention/archive`, optionally
with an `olderThan` duration. Archived rows retain sequence, links, event
hashes, payload digests, an archive ID, and a manifest hash, while the payload
itself is removed. Verification validates the manifest and links across the
archive boundary and only skips recomputing an archived payload hash when that
proof is intact. A direct database change to an active row or archive metadata
therefore produces the first violating sequence rather than a false positive.

Redaction is append-only: the original hash and payload are not rewritten.
The redaction event carries the original hash and a deterministic marker
payload, preserving chain integrity but not making the original database
value unrecoverable. Access control and secure physical deletion remain
deployment responsibilities.

## Scenario C clarification and scope

`SCENARIO_C.md` normalizes the ambiguous requirement that regulators must audit
access to client account data. It maps compliance evidence to the Scenario A
event contract and Scenario B lifecycle/export controls, while explicitly
scoping out authentication, regulator-specific reporting, legal holds, and
jurisdictional policy until those decisions receive human approval.

## Test

```bash
mvn test
```

Tests use H2 with the same Flyway migration, so a PostgreSQL or Kafka process is
not required for the test suite.

## Integrity limitations and next phases

This MVP provides tamper evidence within the database: changing a row breaks
the chain verification. It does not provide external immutability, trusted
time, key-based signatures, cross-region replication, or an independent
checkpoint. A database administrator with write access can rewrite all rows and
recompute hashes. Production hardening should add restricted database roles,
operational monitoring, and an external checkpoint/attestation mechanism.

Future phases can add an outbox-backed Kafka adapter (without making append
availability depend on Kafka), external checkpoint publication and verification,
retention/archival policy, and stronger concurrency/load testing.

## Submission documentation

* [`ATTESTATION.md`](ATTESTATION.md) — engineer assignment attestation.
* [`FINAL_ENGINEERING_SUMMARY.md`](FINAL_ENGINEERING_SUMMARY.md) — final plan,
  artifacts, validation, risks, assumptions, and limitations.
* [`SETUP.md`](SETUP.md) — local setup and end-to-end verification.
