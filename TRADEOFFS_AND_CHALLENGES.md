# Cross-Scenario Trade-offs and Challenges

This document is the shared reference for design choices spanning Scenarios
A, B, and C. It separates assignment-complete behavior from controls that
require production decisions or infrastructure.

## Alignment summary

| Scenario | Requirement | Repository alignment | Boundary |
| --- | --- | --- | --- |
| A | Append structured events, query with combined filters and pagination, and verify a hash chain | Implemented through REST APIs, PostgreSQL/Flyway schema, canonical JSON, idempotency, and verification | Authentication, external anchoring, and Kafka are not implemented |
| B | Retention, redaction without breaking integrity, and verifiable actor/resource export | Implemented with archive manifests, compensating redaction events, payload digests, and export boundary metadata | WORM, encryption/key management, legal holds, and scheduled policy enforcement are deferred |
| C | Clarify “audit access to client account data” and translate it into design | Clarified requirement, assumptions, design mapping, scope, and acceptance criteria documented | Compliance UI, regulator-specific reports, authorization, and jurisdiction policy are deferred |

## Core design trade-offs

| Decision | Chosen approach | Benefit | Cost or risk |
| --- | --- | --- | --- |
| Ordering | Per tenant and logical stream | Scales better than one global sequence and isolates unrelated traffic | Cross-stream total ordering is unavailable |
| Timestamp | Preserve caller timestamp when supplied; assign server time otherwise | Retains business event time while guaranteeing a usable value | Caller clocks may be inaccurate; sequence remains the ordering authority |
| Hash input | Canonical JSON payload digest plus envelope metadata and previous hash | Archived payloads can be verified without retaining sensitive JSON; algorithm versions permit evolution | Plain SHA-256 digests can be guessed for low-entropy secrets; keyed protection is a production follow-up |
| Legacy compatibility | Store algorithm version and verify old rows with their original method | Historical records remain verifiable without rewriting them | Verifier and migration logic must support multiple algorithms |
| Retention | Archive only a contiguous old prefix into a proof-bearing archive table | Keeps the active table smaller while preserving chain continuity | Middle-of-chain archival is not supported; archive reads are more complex |
| Redaction | Append a compensating `audit.redaction` event with deterministic `[REDACTED]` markers | Original hash remains valid and the privacy action is auditable | Original values remain in backups, replicas, logs, and pre-redaction exports |
| Export | Filtered bundle with event hashes and stream boundary metadata | Efficient actor/resource transfer with honest completeness signaling | Recipient needs a trusted boundary hash for full-stream independence |
| Delivery | At-least-once/idempotent model | Resilient retries and simpler integration semantics | Consumers must deduplicate; exactly-once is not claimed |
| Database | PostgreSQL as transactional authority | Strong constraints, ordered queries, and Flyway migrations | A privileged database operator can rewrite all rows unless external attestations exist |

## Main engineering challenges

### Concurrency and sequence allocation

Two writers targeting one stream must not produce duplicate or skipped
sequences. The implementation locks the current tail and relies on database
constraints. Production load testing must cover concurrent appends,
deadlocks, retries, and transaction rollback.

### Detectability versus prevention

A local hash chain detects changes only when verification has access to a
trusted reference. An operator able to rewrite every row can recompute the
chain. Independent signed checkpoints and immutable/WORM retention are needed
for stronger evidence.

### Privacy versus independent verification

Removing a payload invalidates a raw-payload hash. The selected solution stores
a payload digest and algorithm version, removes payload JSON during archival,
and records redaction as a new event. This preserves evidence but does not
guarantee erasure from all copies. Encryption, keyed HMACs, key destruction,
and legal-hold rules require explicit policy.

### Partial export verification

Actor/resource exports are filtered subsets, not necessarily complete streams.
The bundle therefore reports `completeChain` and includes the preceding and
terminal hashes. Independent recipients still need a trusted checkpoint or
full-stream source to prove the selected range was not omitted from.

### Ambiguous compliance semantics

“Access” can mean reads, exports, failed attempts, background jobs, or
administrative actions. Scenario C records the questions and an interim
assumption instead of silently hard-coding a regulatory interpretation.

### Operational and security gaps

The assignment slice does not yet provide authentication, authorization,
tenant isolation enforcement, metrics, alerting, external checkpoint storage,
key management, legal holds, or deployment automation. These are release
gates, not documentation-only enhancements.

## Validation and safety guardrails

* Verify normal append, retry, filtering, pagination, archive, redaction, and
  export flows.
* Mutate payload, metadata, event hash, previous hash, delete a row, reorder
  rows, and alter archive manifests in a test database; verification must
  report the first violation.
* Test migration upgrades from legacy hash rows to versioned rows without
  changing historical hashes.
* Test concurrent appends and transaction failures against PostgreSQL.
* Do not use production payloads, credentials, private keys, or personal data
  in AI prompts or test fixtures.
* Require human approval for hash algorithms, schema migrations, retention,
  redaction, authorization, key management, and deployment changes.

## Release decision

Scenarios A, B, and C are aligned at the documented assignment scope. The
service is suitable as an implementation foundation and evaluation artifact,
not as a production compliance system until the deferred security,
availability, privacy, and regulatory controls are approved and validated.
