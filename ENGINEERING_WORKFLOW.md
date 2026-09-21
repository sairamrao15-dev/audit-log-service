# Engineering Workflow and AI Governance

## 1. Requirement understanding

The engineering problem is to build a multi-tenant, append-only audit-log
service that accepts events from multiple channels, preserves deterministic
ordering, makes tampering detectable, and remains operable at scale. The
authoritative append path must be shared by REST, Kafka, and future adapters.
An audit event cannot be updated or physically deleted through the product
APIs; corrections are new events.

The following decisions are explicit assumptions until a product owner signs
off on them:

* ordering is per tenant and logical stream;
* delivery is at-least-once with idempotent consumers;
* PostgreSQL is the query/transaction authority;
* independently verifiable integrity requires signed checkpoints retained
  outside the database;
* producer time and server receipt time are both retained, while sequence
  controls ordering;
* tenant authorization is required for every append, read, verify, and export.

Unresolved decisions are tracked in the README ambiguity table and must not be
silently inferred in production-critical code.

## 2. Task decomposition and sequencing

Every implementation task must record intent, constraints, technical context,
acceptance criteria, owner, and status. The current dependency order is:

| ID | Task | Depends on | Acceptance criteria |
| --- | --- | --- | --- |
| FND | Foundation and domain model | — | Build, configuration, migrations, event envelope, versioning, and local test harness exist. |
| INT | Append integrity core | FND | Transactional append, per-stream sequencing, canonicalization, hash chain, idempotency, and no mutation API pass tests. |
| REST | REST integration | INT | Authenticated append/query/verify APIs, OpenAPI contract, pagination, validation, and error semantics are tested. |
| KAFKA | Kafka integration | INT | Versioned topics, partition strategy, retries/DLQ, duplicate handling, and observability are tested with a broker. |
| ATT | Attestation and archive | INT | Signed checkpoints, immutable retention, key IDs/rotation, and independent verification are tested. |
| OPS | Production operations | REST, KAFKA, ATT | Metrics, traces, alerts, replay, backup/restore verification, Kubernetes deployment, load, and failure tests pass. |
| REVIEW | Release review | OPS | Security, performance, documentation, risk, and human sign-off gates are complete. |

No dependent task is considered complete based only on generated code; its
acceptance criteria and quality gates must be recorded.

## 3. AI-assisted execution protocol

AI may assist with analysis, implementation, debugging, refactoring, test
generation, documentation, and review preparation. The engineer remains the
owner of correctness, maintainability, security, and production readiness.

For each AI-assisted change:

1. State the task intent, relevant constraints, acceptance criteria, and files
   in scope before prompting.
2. Ask for a focused change, not broad autonomous modification.
3. Inspect generated output and refine it iteratively against the acceptance
   criteria.
4. Run applicable quality gates and review the diff as an engineer.
5. Record the result as **generated**, **edited**, or **rejected**, including
   rationale and validation evidence, in `AI_USAGE_LOG.md`.

AI must not receive credentials, production payloads, private keys, tokens, or
unredacted personal data. Prompts and generated artifacts must not weaken
authorization, disable integrity checks, bypass tests, or claim guarantees
that the implementation does not provide.

## 4. Quality gates

| Gate | Required evidence |
| --- | --- |
| Analysis | Requirements, assumptions, dependencies, and risks are documented. |
| Design | API, schema, ordering, delivery, and integrity contracts are reviewed. |
| Build/type safety | Clean compile and dependency resolution on the supported JDK. |
| Tests | Focused unit tests plus PostgreSQL/Kafka integration tests for changed behavior. |
| Security | Dependency scan, secret scan, authorization tests, threat-model review, and safe logging. |
| Performance | Load measurements for append latency/throughput, query limits, consumer lag, and checkpoint work. |
| Reliability | Retry, duplicate, interruption, rollback, replay, backup/restore, and failure-injection scenarios. |
| Documentation | API/schema contracts, runbooks, limitations, and migration notes are current. |
| Human approval | Engineer sign-off is recorded for schema, authorization, integrity, retention, and deployment changes. |

If a gate cannot run in the local environment, the work remains explicitly
blocked or is validated in CI; a missing tool is not evidence of success.

## 5. Risk control and human oversight

High-impact changes require human approval before merge or deployment:

* event hash/canonicalization algorithms and database migrations;
* authentication, authorization, tenant isolation, and retention behavior;
* checkpoint signing, key management, archival, and restore procedures;
* Kafka ordering, replay, DLQ, and delivery semantics;
* production infrastructure, data-access roles, and observability changes.

Known risks include database administrators rewriting rows and recomputing a
local chain, checkpoint loss, clock differences, hot streams, unbounded
payloads, duplicate delivery, schema incompatibility, and incomplete restore
verification. Each risk needs a mitigation, an owner, and a test or operational
control before production release.

## 6. Final engineering summary

Every release or major task completion must include:

* plan and rationale;
* artifacts changed (code, APIs, schemas, tests, and documentation);
* AI assistance used and accepted/rejected decisions;
* risks, trade-offs, assumptions, and known limitations;
* validation results and any blocked gates;
* required follow-up work;
* named engineer approval for high-impact changes.

