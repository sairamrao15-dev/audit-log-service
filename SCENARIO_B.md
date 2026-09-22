# Scenario B — Retention, Redaction, and Bulk Export

## Requirement

Extend Scenario A so approved retention, privacy, and evidence-transfer
operations preserve tamper evidence without exposing unnecessary sensitive
data.

## Scope

Included:

* configurable archival of records older than policy;
* verification that treats legitimate archival as continuous;
* structured field redaction without rewriting historical event hashes;
* actor- or resource-scoped self-contained export bundles;
* documentation of privacy, integrity, and independent-verification limits.

## Acceptance summary

1. Records beyond the retention window can be archived.
2. Verification remains valid after legitimate archival.
3. Archive metadata or chain tampering is reported.
4. Redaction creates an auditable append-only result and does not invalidate
   the historical chain.
5. Export bundles identify their scope and provide enough chain metadata for
   recipient verification.
6. The service does not claim that redaction removes values from backups,
   replicas, logs, or earlier exports.

## Technical reference

All retention schema, archive-manifest design, payload-digest hash version,
redaction algorithm, export contract, NFRs, quality gates, risks, and
production limitations are defined in
[`REQUIREMENTS_HLD_LLD.md`](REQUIREMENTS_HLD_LLD.md).
The shared trade-off register is
[`TRADEOFFS_AND_CHALLENGES.md`](TRADEOFFS_AND_CHALLENGES.md).
