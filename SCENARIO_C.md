# Scenario C — requirements clarification and scope

## Decision

Scenario C covers operational lifecycle controls around an append-only audit
chain: retention, archival, structured redaction, and export. The source of
truth remains the tenant/stream database chain. Archive rows retain enough
metadata to prove sequence and hash continuity while omitting payload bytes.
An archive manifest is checked before verification skips an archived payload.

## In scope

* Configurable retention cutoff and an explicit archive operation.
* SHA-256 archive manifests bound to event identity, envelope metadata, links,
  event hashes, and canonical payload digests.
* A deterministic `[REDACTED]` marker and compensating redaction event.
* Resource- or actor-scoped JSON export bundles with chain boundary metadata.
* First-violation reporting for active rows, archive rows, links, gaps, and
  manifests.

## Out of scope / required before production

This is not a claim of independent immutability. WORM/object-lock storage,
external signatures, key rotation, encryption, authorization policy,
cross-region replication, legal-hold workflows, and an independently operated
checkpoint service need a deployment-specific design and approval. Redaction
does not make a value unrecoverable from backups or pre-archive copies.

## Compatibility

The original `occurredAt` request and response name remains supported as an
alias for `timestamp`. Existing events with no new envelope fields retain the
original hash input shape; events using actor/resource fields use the extended
hash input shape without rewriting historical rows.
