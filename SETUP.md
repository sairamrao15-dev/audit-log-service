# Local Setup and Verification

This guide runs the audit-log service locally and exercises the core
append/query/verify flow. The service is a Java 21 and Maven application with
PostgreSQL as its runtime database. Kafka is an extension seam and is not
required for the current REST prototype.

## Prerequisites

Install:

* JDK 21
* Maven 3.9 or newer
* PostgreSQL 15 or newer

The following database values are used by default:

| Setting | Default |
| --- | --- |
| JDBC URL | `jdbc:postgresql://localhost:5432/auditlog` |
| Username | `auditlog` |
| Password | `auditlog` |

Create the database and user before starting the application:

```sql
CREATE USER auditlog WITH PASSWORD 'auditlog';
CREATE DATABASE auditlog OWNER auditlog;
```

Do not use these example credentials in a shared or production environment.

## Configure and run

From the repository root, set environment variables when the defaults are not
appropriate:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/auditlog
export DB_USERNAME=auditlog
export DB_PASSWORD=change-me
```

On Windows PowerShell, use:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/auditlog"
$env:DB_USERNAME = "auditlog"
$env:DB_PASSWORD = "change-me"
```

Start the service:

```bash
mvn spring-boot:run
```

Flyway applies the versioned migrations during startup. The application
listens on port `8080` unless the Spring Boot port is overridden.

## Kafka delivery modes and consumer idempotency

Kafka is not enabled in the current prototype (`audit-log.kafka.enabled` is
`false`), and no broker is required for the REST flow. When the Kafka adapter
is enabled, choose the delivery mode deliberately. "At-most-once" and
"at-least-once" describe delivery attempts; neither mode alone makes a
consumer's side effects safe from duplicates.

### At-most-once delivery

Use this when losing an event is preferable to retrying it. Commit the Kafka
offset before processing the record. A process crash after the commit can lose
the event, but a committed record will not normally be delivered again:

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
    listener:
      ack-mode: manual
      # The consumer must acknowledge/commit before invoking business logic.
```

The application listener must explicitly commit the offset before invoking
the audit append operation. This mode is not suitable when every audit event
is required for compliance evidence because a crash can create a gap.

### At-least-once delivery (recommended for audit events)

Use this when every event must be attempted and a crash may cause a replay.
Disable auto-commit, process the event, persist the audit result, and commit
the Kafka offset only after successful processing:

```yaml
spring:
  kafka:
    consumer:
      enable-auto-commit: false
      properties:
        isolation.level: read_committed
    listener:
      ack-mode: record
      # Commit succeeds only after the listener returns successfully.
```

The exact listener acknowledgement mode is adapter-specific. The important
guarantee is that the offset is committed after the authoritative database
transaction, not before it. Kafka producer settings should use
`acks=all` and idempotence when the service publishes downstream events:

```yaml
spring:
  kafka:
    producer:
      properties:
        acks: all
        enable.idempotence: true
```

### Consumer-side idempotency

At-least-once delivery requires every consumer to deduplicate before applying
side effects. Use the immutable audit `eventId` as the primary deduplication
key, together with the tenant and stream when the consumer's namespace is
scoped. If the source message carries an `Idempotency-Key`, retain that value
as an additional diagnostic and replay key; do not rely on an in-memory set.

The consumer should:

1. validate the event envelope and reject malformed messages to a monitored
   dead-letter topic;
2. insert `(consumer_name, tenant_id, event_id)` into a durable inbox table
   with a unique constraint;
3. apply the business side effect and mark the inbox record processed in the
   same database transaction where possible;
4. acknowledge the Kafka offset only after that transaction commits;
5. treat a unique-constraint conflict for an already processed `eventId` as a
   successful duplicate and acknowledge it without applying the side effect
   again.

This protects against consumer restarts, redelivery, partition rebalances,
and producer retries. It does not provide global exactly-once behavior across
Kafka and an unrelated external system; such integrations need their own
idempotency key or transactional outbox.

## Smoke test

Append an event. The idempotency key must be unique for the selected tenant
and stream:

```bash
curl -X POST "http://localhost:8080/v1/tenants/demo/streams/accounts/events" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: smoke-001" \
  -d '{
    "eventType": "RECORD_UPDATED",
    "actorId": "service-smoke",
    "resourceType": "account",
    "resourceId": "account-001",
    "payload": {"operation": "status-change"},
    "timestamp": "2026-09-22T12:00:00Z"
  }'
```

Query the stream:

```bash
curl "http://localhost:8080/v1/tenants/demo/streams/accounts/events?resourceId=account-001&limit=20"
```

Verify the complete local chain:

```bash
curl "http://localhost:8080/v1/tenants/demo/streams/accounts/verify"
```

The equivalent assignment endpoint is:

```bash
curl "http://localhost:8080/audit/verify?tenantId=demo&streamId=accounts"
```

The response reports `intact`, the event count, and the first invalid sequence
and violation type when a problem is detected.

## Run tests

```bash
mvn test
```

Tests use H2 and Flyway, so a PostgreSQL or Kafka process is not required for
the test suite. PostgreSQL-backed migration, concurrency, and failure tests
remain deployment/CI quality gates.

## Scenario B operations

Archive an eligible contiguous prefix:

```bash
curl -X POST \
  "http://localhost:8080/v1/tenants/demo/streams/accounts/retention/archive?olderThan=365d"
```

Export events by actor or resource:

```bash
curl "http://localhost:8080/audit/export?tenantId=demo&resourceId=account-001"
```

Append a redaction event for selected payload paths:

```bash
curl -X POST \
  "http://localhost:8080/v1/tenants/demo/events/{eventId}/redactions" \
  -H "Content-Type: application/json" \
  -d '{"paths":["accountNumber","customer.identifier"]}'
```

Redaction is represented by a compensating event. It does not rewrite the
original event or claim erasure from backups, replicas, logs, or prior exports.

## Troubleshooting

* **Database connection failure:** confirm PostgreSQL is running, the database
  exists, and `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` are set in the same
  shell used to start Maven.
* **Migration failure:** inspect the first Flyway error and restore the
  database to a known state before retrying; do not edit historical audit rows
  to make a migration pass.
* **Duplicate append:** reuse of an idempotency key with the same content is
  safe; reuse with different content returns a conflict.
* **Broken verification:** preserve the database state for investigation and
  identify the first reported sequence. Do not repair source records by
  recomputing hashes.

For the complete API, schema, hash contract, non-functional requirements, and
production boundaries, see `REQUIREMENTS_HLD_LLD.md`.
