# Scenario A — Core Audit Log Service

## Requirement

Build a tenant-scoped, append-only audit-log service that accepts structured
events, supports combined filtering and pagination, and detects tampering
through a sequential hash chain. The assignment is validated by writing,
querying, verifying, directly modifying a test database record, and verifying
again.

## Scope

Included:

* event write and batch-write APIs;
* event type, actor, resource, payload, timestamp, and immutable identity;
* append-only behavior with idempotent retries;
* actor/resource/event/time-range filters and pagination;
* chain verification with first-failure reporting;
* direct-database tamper detection.

Excluded from this scenario: retention lifecycle, redaction, bulk export, and
regulatory reporting policy. Those are covered at a high level in Scenarios B
and C.

## Acceptance summary

1. A valid event can be written and queried.
2. Multiple filters and pagination return the expected records.
3. An intact stream reports successful verification.
4. A direct payload, metadata, hash, link, deletion, insertion, or reorder
   mutation reports an invalid chain and first affected sequence.
5. No update or delete event endpoint exists.

## Technical reference

All functional and non-functional requirements, HLD, LLD, schema, hash
algorithm, API contracts, task decomposition, quality gates, risks, and
oversight controls are maintained in
[`REQUIREMENTS_HLD_LLD.md`](REQUIREMENTS_HLD_LLD.md).
