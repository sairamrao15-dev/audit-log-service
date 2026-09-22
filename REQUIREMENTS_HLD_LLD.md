# Core Audit Log Requirements, HLD, and LLD

## 1. Engineering problem

Build a tenant-scoped, append-only audit-log service that accepts structured
events, supports filtered and paginated retrieval, and detects unauthorized
modification, deletion, insertion, or reordering through a cryptographic hash
chain.

The core assignment is validated through:

1. writing events;
2. querying events with filters and pagination;
3. verifying an intact chain;
4. modifying a record directly in a test data store;
5. verifying that the first inconsistency is reported.

No external consumer is required for the core scenario.

## 2. Core requirements

### Event write contract

`POST /v1/tenants/{tenantId}/streams/{streamId}/events`

Required event fields:

| Field | Required | Description |
| --- | --- | --- |
| `eventType` | Yes | Action such as `USER_LOGIN`, `RECORD_UPDATED`, or `PERMISSION_GRANTED`. |
| `actorId` | Yes for new integrations | User, service, or process responsible for the event. |
| `resourceType` | Yes for new integrations | Type of affected resource. |
| `resourceId` | Yes for new integrations | Identifier of affected resource. |
| `payload` | Yes | Structured event-specific JSON object. |
| `timestamp` | No | Caller-supplied event timestamp. |

If `timestamp` is omitted, the service assigns the server append time. The
legacy `occurredAt` field is accepted as an alias. Timestamp values are
truncated to database precision before hashing. Sequence number defines stream
ordering; timestamps are event metadata.

Each request requires an `Idempotency-Key`. The response contains the event
ID, stream sequence, previous hash, event hash, payload digest, and algorithm
version.

### Append-only behavior

The API exposes append, query, lookup, and verification operations only. There
are no update or delete endpoints. Corrections are represented as new
compensating events. Reusing an idempotency key with different content returns
`409 Conflict`.

### Query contract

`GET /v1/tenants/{tenantId}/streams/{streamId}/events`

Any combination of these filters is supported:

* `actorId`
* `resourceType`
* `resourceId`
* `eventType`
* `from` inclusive timestamp
* `to` inclusive timestamp

Pagination uses bounded `limit` and either zero-based `page` or direct
`offset`. Results are ordered by ascending stream sequence. Archived rows are
returned through the same logical query surface.

### Verification contract

`GET /audit/verify?tenantId={tenantId}&streamId={streamId}`

The response reports:

* whether the chain is intact;
* total event count;
* `firstInvalidSequence`, when invalid;
* violation type, such as sequence gap, previous-hash mismatch, payload digest
  mismatch, event-hash mismatch, or archive-manifest mismatch.

The tenant-scoped stream verification endpoint is also available:

`GET /v1/tenants/{tenantId}/streams/{streamId}/verify`

## 3. Cross-scenario functional requirements

### Scenario A — Core audit logging

The service shall accept structured events, assign immutable identity and
stream sequence, persist a verifiable hash chain, support combined filters and
bounded pagination, and report the first integrity violation. It shall not
expose event update or delete operations. Retries shall be safe through
idempotency.

### Scenario B — Retention, redaction, and export

The service shall:

* archive records older than the approved retention window without creating a
  false chain break;
* preserve enough archive metadata to verify sequence and integrity after
  payload removal;
* support structured sensitive-field redaction without rewriting historical
  event hashes;
* export actor- or resource-scoped records with enough metadata for an
  independent recipient to verify included records and chain boundaries.

### Scenario C — Compliance reporting

The service shall provide the evidence foundation for authorized auditing of
client-account access. Access, export, administrative, and permission events
must be representable with actor, resource, action, outcome, timestamps, and
correlation context. Compliance users must be able to query, verify, and
export evidence within tenant boundaries. Regulatory retention, legal hold,
identity, and report-format decisions remain policy inputs.

## 4. Non-functional requirements

| Category | Requirement | Engineering target / acceptance evidence |
| --- | --- | --- |
| Integrity | Unauthorized alteration, deletion, insertion, and reordering must be detectable | Full-chain verification identifies the first invalid sequence and violation |
| Durability | A committed append must survive process restart and database failover according to the deployment RPO | PostgreSQL transaction tests, backup/restore verification, and documented RPO |
| Availability | Reads and writes should remain independently operable where possible | Health/readiness checks, transaction rollback tests, and defined RTO |
| Consistency | One stream has deterministic sequence order and idempotent retries | Concurrent PostgreSQL append tests and uniqueness constraints |
| Performance | Query and export work must be bounded and predictable | Configured limits, indexed filters, load tests for append/query/verify |
| Scalability | Independent streams can scale horizontally without global ordering | Stream-partitioned workload tests and no global sequence lock |
| Security | Tenant boundaries, least privilege, safe logging, and authenticated operations are required before production | Authorization tests, secret/dependency scans, threat model, restricted DB roles |
| Privacy | Sensitive values must not be unnecessarily exposed in logs, archives, AI prompts, or exports | Redaction tests, payload limits, data-classification review, key policy |
| Compatibility | API, schema, canonicalization, and hash changes must be versioned | Flyway upgrade tests and algorithm-version compatibility tests |
| Observability | Operators need append failures, latency, lag, verification failures, archive age, and export activity | Structured logs, metrics, traces, dashboards, and alerts |
| Reliability | Retries, duplicate delivery, rollback, archive movement, restore, and replay must be safe | Failure-injection and recovery tests |
| Operability | Retention, verification, export, and incident response must be runbook-driven | Versioned runbooks and operator drills |
| Compliance | Evidence access must be auditable and policy-controlled | Compliance access events, retention/legal-hold approval, and audit review |
| Maintainability | Domain rules must be centralized and adapters must not duplicate integrity logic | Unit-testable domain services, clean package boundaries, code review |
| Resource safety | Payload, batch, nesting, page, export, and verification workloads must be capped | Validation tests and load-shedding behavior |

Targets such as exact throughput, p95 latency, RPO/RTO, retention duration,
regional availability, and regulatory scope must be approved per deployment;
they must not be invented by the implementation.

## 5. High-level design

### Architecture

```text
REST client
    |
    v
Spring MVC controllers
    | request validation and API error mapping
    v
AuditEventService
    | append / idempotency / sequencing
    | hashing / verification / archive / export orchestration
    v
JdbcAuditEventRepository
    | parameterized SQL and transaction participation
    v
PostgreSQL
    | audit_events
    | audit_event_archives
    | Flyway-managed schema
```

### Component responsibilities

| Component | Responsibility | Explicit non-responsibility |
| --- | --- | --- |
| `AuditEventController` | Write, batch, query, lookup, verify, and redaction HTTP endpoints | No hashing or SQL |
| `AuditOperationsController` | Global verification and actor/resource export endpoints | No direct persistence |
| `RetentionController` | Explicit archive operation and retention input validation | No unreviewed scheduled deletion |
| `AuditEventService` | Domain workflow, idempotency, sequence, append, export, redaction, archival | No HTTP concerns |
| `HashChainHasher` | Canonical payload, payload digest, event hash, archive manifest | No persistence |
| `HashChainVerifier` | Ordered chain walk and first-failure result | Never mutates data |
| `JdbcAuditEventRepository` | Parameterized queries, inserts, archive reads/writes | No public update/delete event operation |
| PostgreSQL | Durable storage, constraints, indexes, transaction isolation | Not an independent tamper anchor |
| Flyway | Forward-only schema migration | Does not rewrite historical hashes |

### Data flow

1. Validate the HTTP request and idempotency header.
2. Convert the request into an internal append command.
3. Canonicalize the payload and calculate its digest.
4. Look up an existing idempotency key.
5. Lock the current stream tail in the append transaction.
6. Assign the next sequence and previous hash.
7. Compute and insert the immutable event.
8. Commit and return the stored representation.

Future Kafka, gRPC, webhook, CDC, and SDK adapters must call the same service
command boundary rather than duplicate this flow.

## 6. Low-level design

### Package structure

```text
com.example.auditlog
├── api
│   ├── AuditEventController
│   ├── AuditOperationsController
│   ├── RetentionController
│   ├── request/response records
│   └── ApiExceptionHandler
├── domain
│   ├── AuditEvent
│   ├── AppendEventCommand
│   ├── CanonicalJson
│   ├── HashChainHasher
│   ├── HashChainVerifier
│   ├── PayloadRedactor
│   └── VerificationResult
├── persistence
│   ├── AuditEventRepository
│   └── JdbcAuditEventRepository
├── service
│   └── AuditEventService
├── adapter
│   ├── AuditEventPublisher
│   └── NoOpAuditEventPublisher
└── checkpoint
    └── CheckpointService
```

### Relational schema

`audit_events` contains:

* primary key `(tenant_id, stream_id, sequence_number)`;
* unique immutable `event_id`;
* unique `(tenant_id, stream_id, idempotency_key)`;
* event type, actor/resource metadata, timestamp, payload JSON;
* `previous_hash`, `event_hash`, `payload_hash`, `hash_algorithm`;
* server `created_at`.

`audit_event_archives` contains the same stream and chain metadata but omits
payload JSON. It additionally stores `archive_id` and
`archive_manifest_hash`. Active and archive rows are combined into one logical
ordered stream for query and verification.

Flyway migrations:

* `V1__create_audit_events.sql` — initial event table and constraints;
* `V2__extend_events_and_archives.sql` — actor/resource fields and archive
  table;
* `V3__version_hash_algorithm.sql` — payload digest and algorithm versioning.

### Versioned hash contract

New events use `SHA-256-PAYLOAD-DIGEST-V2`:

```text
canonicalPayload = sortObjectKeys(payload)
payloadHash = SHA-256(canonicalPayload)

eventHash = SHA-256(
  lengthPrefixed(tenantId) |
  lengthPrefixed(streamId) |
  lengthPrefixed(sequenceNumber) |
  lengthPrefixed(eventId) |
  lengthPrefixed(eventType) |
  lengthPrefixed(actorId) |
  lengthPrefixed(resourceType) |
  lengthPrefixed(resourceId) |
  lengthPrefixed(timestamp) |
  lengthPrefixed(payloadHash) |
  lengthPrefixed(previousHash)
)
```

Length-prefixing prevents delimiter ambiguity. The first stream event uses a
null previous hash as the genesis value. Historical rows using
`SHA-256-RAW-PAYLOAD-V1` continue to use their original verification method.
Algorithm identifiers prevent silent reinterpretation of stored hashes.

### Append transaction and concurrency

```text
BEGIN
  existing = findByIdempotencyKey(tenant, stream, key)
  if existing:
      compare immutable request content
      return existing or conflict

  tail = select latest stream row for update
  sequence = tail.sequence + 1 or 1
  previousHash = tail.eventHash or null
  calculate payloadHash and eventHash
  INSERT event
COMMIT
```

Database constraints handle event ID, idempotency, and sequence races. A
duplicate-key race reloads the winning idempotency record. Concurrent writers
to one stream are serialized at the tail; different streams can proceed
independently.

### Verification algorithm

```text
expectedSequence = 1
previousHash = null

for event ordered by sequence:
    require event.sequence == expectedSequence
    require event.previousHash == previousHash

    if active:
        require SHA-256(canonicalPayload) == event.payloadHash
        require recomputed eventHash == event.eventHash
    else:
        require archive metadata and manifest are valid

    previousHash = event.eventHash
    expectedSequence += 1
```

The verifier returns on the first failed requirement and never modifies
records. Direct changes to payload, metadata, event hash, previous hash,
deletion, insertion, or ordering must fail verification.

### API validation and error mapping

* Missing/blank idempotency keys: `400 Bad Request`.
* Missing event type or payload: `400 Bad Request`.
* Invalid page, offset, limit, retention, selector, or redaction path:
  `400 Bad Request`.
* Unknown event ID: `404 Not Found`.
* Conflicting idempotency reuse: `409 Conflict`.
* Errors use `ProblemDetail` and do not expose SQL, stack traces, credentials,
  or payload secrets.

## 7. Scenario alignment

Scenario A defines the event, append, query, and verification foundation.
Scenario B extends it with retention archival, structured redaction, payload
digest hashing, and verifiable exports. Scenario C maps the ambiguous
regulatory access requirement to these primitives and identifies the required
authorization and reporting layer.

See:

* [`SCENARIO_A.md`](SCENARIO_A.md) — assignment narrative and validation flow;
* [`SCENARIO_B.md`](SCENARIO_B.md) — retention, redaction, and export design;
* [`SCENARIO_C.md`](SCENARIO_C.md) — compliance clarification and scope;
* [`TRADEOFFS_AND_CHALLENGES.md`](TRADEOFFS_AND_CHALLENGES.md) — shared design
  decisions, trade-offs, and risks.

## 8. Engineering execution plan

| ID | Intent | Depends on | Acceptance criteria |
| --- | --- | --- | --- |
| CORE-REQ | Normalize event, timestamp, ordering, and integrity requirements | — | Ambiguities and assumptions are documented and approved |
| CORE-DESIGN | Review HLD, LLD, API, schema, and hash contract | CORE-REQ | Design decision and threat-model review complete |
| CORE-SCHEMA | Implement Flyway schema and constraints | CORE-DESIGN | Fresh install and upgrade migrations pass |
| CORE-HASH | Implement canonicalization, payload digest, event hash, and verifier | CORE-SCHEMA | Unit tests prove deterministic hashes and first-failure reporting |
| CORE-APPEND | Implement transaction, sequencing, idempotency, and repository | CORE-HASH | Concurrent and retry tests pass |
| CORE-API | Implement write, query, lookup, and verify APIs | CORE-APPEND | API contract tests pass; no mutation routes exist |
| CORE-TAMPER | Test direct database mutation and removal scenarios | CORE-API | Payload, metadata, link, deletion, and reorder tampering is detected |
| CORE-GATES | Run build, test, security, performance, and reliability gates | CORE-TAMPER | Evidence is recorded; blocked gates are explicit |
| CORE-SIGNOFF | Human approval of production-impacting decisions | CORE-GATES | Named engineer approves correctness and readiness |
| KAFKA | Add versioned Kafka ingress/egress adapters | CORE-SIGNOFF | Partitioning, retries, DLQ, duplicate delivery, ordering, schema compatibility, and observability pass broker-backed tests |
| OPS | Harden production operations | KAFKA | Authentication/authorization, metrics, tracing, Kubernetes deployment, backup/restore verification, load/failure tests, and security runbooks pass review |

## 9. AI-assisted execution controls

AI may assist with analysis, design, implementation, debugging, refactoring,
test generation, documentation, and review preparation. Engineers retain
ownership of correctness, maintainability, security, and production readiness.

For every AI-assisted task:

1. define intent, constraints, context, files, and acceptance criteria;
2. request a focused change;
3. iteratively refine and inspect the generated result;
4. run applicable quality gates;
5. record `generated`, `edited`, or `rejected` status and rationale in
   `AI_USAGE_LOG.md`.

Do not provide credentials, tokens, private keys, production payloads, or
unredacted personal data to AI tools. AI output must not weaken authorization,
disable integrity checks, bypass tests, or overstate guarantees.

## 10. Quality gates, risks, and oversight

Required gates:

| Gate | Evidence |
| --- | --- |
| Requirements/design | Current scenario contracts, assumptions, and dependency plan |
| Build | Maven build succeeds on Java 21 |
| Unit | Canonicalization, algorithms, sequencing, idempotency, redaction, verifier |
| Integration | PostgreSQL/Flyway migrations, transactions, concurrency, tampering |
| API | Filters, pagination, errors, verification, unsupported mutations |
| Security | Secret/dependency scans, authorization review, safe logging |
| Performance | Append throughput, stream contention, query limits, verify cost |
| Reliability | Retry, rollback, database outage, archive, restore, and replay |
| Documentation | APIs, schema, runbooks, scenarios, limitations |
| Human sign-off | Hash, schema, retention, redaction, authorization, and deployment approval |

Key risks are privileged database rewrites, hot streams, untrusted clocks,
sensitive payload exposure, partial export ambiguity, hash/schema evolution,
tenant isolation failures, unbounded input, Kafka duplicate/replay behavior,
and incomplete operational recovery. Mitigations are documented in
`TRADEOFFS_AND_CHALLENGES.md`.

If a gate cannot run because tooling or infrastructure is unavailable, it is
blocked and recorded rather than treated as passed.

## 11. Final engineering summary

Each milestone or release must include:

* plan and rationale;
* changed code, API, schema, tests, and documentation;
* AI usage and generated/edited/rejected decisions;
* risks, trade-offs, assumptions, and limitations;
* validation evidence and blocked gates;
* follow-up work;
* named engineer approval for high-impact changes.

The current implementation is an assignment-complete foundation, not a
production compliance system. Authentication, authorization, external
attestation, WORM storage, key management, monitoring, recovery validation,
and jurisdiction-specific retention require further implementation and
approval.
