# Scenario C — Compliance Reporting

## Ambiguous requirement

“Regulators need to be able to audit access to client account data.”

## Clarified requirement

For client-account reads, exports, writes, permission changes, and privileged
operations, the service should record who acted, what resource was involved,
what action occurred, the outcome, when it happened, and correlation context.
Authorized compliance users should be able to query, verify, and export that
evidence within tenant boundaries for the approved regulatory retention period.

## Ambiguities requiring product or compliance approval

The implementation needs explicit decisions for:

* what counts as access, including failed reads and background jobs;
* actor identity and authentication mechanism;
* account classes, pseudonymization, and cross-tenant access;
* required evidence, reason/consent data, and clock tolerance;
* compliance-reader authorization and separation of duties;
* retention, legal holds, deletion exceptions, and jurisdiction;
* external signatures, WORM storage, key custody, and report formats.

## Scope decision

This repository implements the reusable evidence foundation from Scenarios A
and B: append-only events, verification, retention-aware archival,
redaction, and verifiable exports. It does not implement a compliance UI,
scheduled regulator reports, authentication/authorization, legal holds,
jurisdiction-specific retention, or external attestation.

## Technical reference

The complete functional and non-functional requirements, HLD, LLD, data model,
security boundaries, acceptance gates, risks, and oversight requirements are
maintained in [`REQUIREMENTS_HLD_LLD.md`](REQUIREMENTS_HLD_LLD.md).
Shared trade-offs and unresolved challenges are in
[`TRADEOFFS_AND_CHALLENGES.md`](TRADEOFFS_AND_CHALLENGES.md).
