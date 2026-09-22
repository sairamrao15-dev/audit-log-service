# Final Engineering Summary

## Plan and rationale

The service was delivered as a Java 21, Spring Boot, Maven application with
PostgreSQL and Flyway. The implementation starts with a transactional,
tenant-and-stream-scoped append path, then layers REST query and verification
capabilities over the same immutable event model. Scenario B extends that
model with archive continuity, append-only redaction, and filtered export.
Scenario C turns the ambiguous compliance request into an explicit evidence
scope rather than silently selecting a regulatory interpretation.

Per-stream sequencing was chosen over global ordering to avoid a single
throughput bottleneck. SHA-256 with canonical payload handling and a
versioned hash algorithm identifier provides deterministic tamper detection
while allowing future hash-contract evolution. PostgreSQL remains the
authoritative transaction boundary; Kafka and external attestation are
documented production extensions, not falsely represented as complete.

## Delivered artifacts

| Area | Artifacts |
| --- | --- |
| Build and runtime | `pom.xml`, Spring Boot application, PostgreSQL configuration, Flyway migrations |
| Core integrity | Append-only event model, idempotency, canonical hashing, chain verification |
| REST API | Append, batch append, query, lookup, verification, retention, redaction, and export endpoints |
| Scenario A | `SCENARIO_A.md` and core API/integrity implementation |
| Scenario B | `SCENARIO_B.md`, archive migration, payload digest versioning, redaction and export behavior |
| Scenario C | `SCENARIO_C.md` with clarified requirement, assumptions, scope, and open decisions |
| Design | `REQUIREMENTS_HLD_LLD.md` and `TRADEOFFS_AND_CHALLENGES.md` |
| Governance | `ENGINEERING_WORKFLOW.md`, `AI_USAGE_LOG.md`, and `ATTESTATION.md` |
| Local operation | `SETUP.md` with prerequisites, startup, smoke tests, and troubleshooting |

## Validation approach

The repository includes focused unit and integration coverage for:

* canonical JSON and payload digest behavior;
* append and idempotency semantics;
* combined filtering and pagination;
* hash-chain verification and tamper detection;
* archive continuity;
* structured redaction;
* actor/resource export bundles.

The intended quality gates also include PostgreSQL migration and concurrency
tests, Kafka broker contract/failure tests, authorization tests, property-based
mutation detection, load testing, failure injection, dependency scanning, and
backup/restore verification.

In the current development environment, Java and Maven were unavailable, so
the automated build and test commands could not be executed locally. This is
explicitly recorded as a validation limitation rather than treated as
successful test evidence. `git diff --check` and source/schema/documentation
inspection were completed for the delivered changes.

## Risks, trade-offs, and limitations

* A local hash chain detects unauthorized changes when the verifier has a
  trusted chain boundary; a privileged operator who rewrites every row can
  recompute a local chain.
* SHA-256 is not encryption. Low-entropy sensitive values may be guessed from
  a digest; production privacy controls need keyed protection or encryption
  with approved key management.
* Archive removes payload JSON but retains proof metadata. Copies in backups,
  replicas, logs, and earlier exports are outside the service's redaction
  boundary.
* Filtered exports are not necessarily complete streams. Bundles report
  contiguity and boundary metadata, but full independent verification requires
  a trusted checkpoint or source covering the omitted range.
* Kafka integration, external signed checkpoints/WORM storage,
  authentication/authorization, Kubernetes deployment, production metrics,
  and regional recovery are not complete in this prototype.
* Throughput, latency, retention duration, RPO/RTO, and availability targets
  remain deployment-specific decisions requiring human approval.

The complete trade-off register and safety guardrails are in
`TRADEOFFS_AND_CHALLENGES.md`.

## Assumptions and ownership

The design assumes per-tenant logical streams, at-least-once integration
delivery with idempotency, PostgreSQL as the source of truth, server-assigned
timestamps when the caller does not provide one, and authorization at every
tenant-scoped operation. These assumptions are documented for review and are
not a substitute for production policy approval.

AI assisted implementation and documentation were inspected and edited by the
engineer. The engineer retains ownership of correctness, security,
maintainability, test evidence, and production readiness. The full
traceability record is in `AI_USAGE_LOG.md`.

## Release position

The repository is a complete assignment-oriented implementation foundation for
Scenarios A, B, and C and is runnable locally when the documented prerequisites
are available. It is not yet a production compliance platform. Production
release requires the deferred security, external attestation, operational,
performance, recovery, and regulatory decisions to be implemented, tested, and
approved.
