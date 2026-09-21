# AI Usage Log

This ledger preserves traceability for AI-assisted engineering work. Entries
must be updated by the responsible engineer, who owns the final result.

## Entry format

```text
### YYYY-MM-DD — <task ID> — <short description>
- Intent:
- Constraints and technical context:
- Prompt/approach summary:
- AI result: generated | edited | rejected
- Engineer changes and rationale:
- Validation performed:
- Human sign-off: <name/role/date or pending>
```

## Recorded entries

### 2026-09-21 — FND/INT/REST — MVP foundation and integrity path
- Intent: Create a runnable audit-log service with append-only storage, hash
  chaining, idempotency, REST APIs, and a documented extension seam.
- Constraints and technical context: Greenfield repository; Java 21, Spring
  Boot, PostgreSQL, Flyway, REST first; Kafka and external attestation must
  not be represented as implemented when they are not.
- Prompt/approach summary: AI was asked to implement the first vertical slice,
  then the generated files and core hashing code were inspected and corrected.
- AI result: edited
- Engineer changes and rationale: Corrected the generated `HashChainHasher`
  method structure so the helper is a valid class method; retained Kafka and
  checkpoint components as explicit disabled/future boundaries.
- Validation performed: Repository diff and source inspection completed.
  Automated build/test execution was blocked because Java and Maven are not
  installed in the execution environment.
- Human sign-off: pending

### 2026-09-21 — REQ — Requirements and governance documentation
- Intent: Normalize greenfield, extension, testing, AI governance, risk, and
  oversight requirements into repository-owned engineering guidance.
- Constraints and technical context: Requirements must distinguish assumptions
  from decisions and preserve engineer ownership.
- Prompt/approach summary: AI generated `README.md` requirements and this
  workflow/ledger structure from the stated requirements.
- AI result: edited
- Engineer changes and rationale: Documentation is explicit about acceptance
  criteria, quality gates, secure prompting, human approval, and limitations.
- Validation performed: Documentation reviewed for traceability and alignment
  with the current MVP.
- Human sign-off: pending

### 2026-09-21 — FND/INT/REST/ATT — Assignment scenarios A, B, and C
- Intent: Extend the MVP to the required audit envelope, filtering,
  verification, retention, redaction, export, and ambiguous-requirement scope.
- Constraints and technical context: Preserve append-only behavior, detect
  direct database mutation, avoid false breaks for policy-approved archival,
  and document the privacy/integrity trade-off for redaction.
- Prompt/approach summary: AI inspected the existing service and generated the
  scenario implementation, tests, migration, README additions, and
  `SCENARIO_C.md`; the implementation was then reviewed for SQL arity and API
  error handling.
- AI result: edited
- Engineer changes and rationale: Corrected the event insert placeholder count
  and added explicit bad-request handling for invalid operation parameters.
- Validation performed: Diff whitespace validation and source/constructor/schema
  inspection completed. Automated tests remain blocked because Java and Maven
  are unavailable.
- Human sign-off: pending

### 2026-09-21 — EXT/ABC — Scenario A/B/C integrity extensions
- Intent: Extend the MVP with envelope filters, append-only verification,
  retention/archive continuity, deterministic redaction, and verifiable export.
- Constraints and technical context: Preserve existing REST compatibility,
  detect direct database mutation, avoid update/delete APIs, and do not claim
  external immutability.
- Prompt/approach summary: AI inspected the existing Java/Spring/Flyway
  implementation, added versioned schema columns/archive rows, and covered
  the new behavior with focused integration tests and scope documentation.
- AI result: edited
- Engineer changes and rationale: Archive manifests retain continuity while
  omitting archived payloads; redaction is a compensating event so historical
  hashes remain valid; exports include boundary metadata for independent
  bundle verification.
- Validation performed: Source and migration inspection completed. Automated
  tests were not run because Java and Maven are unavailable in the execution
  environment.
- Human sign-off: pending

### 2026-09-21 — BUILD — Gradle to Maven migration
- Intent: Replace the Gradle build with a Maven build while preserving the
  Spring Boot, Java 21, Flyway, PostgreSQL, Kafka, H2, and test dependencies.
- Constraints and technical context: Keep source layout and runtime behavior
  unchanged; update developer commands and build documentation.
- Prompt/approach summary: AI converted the Gradle dependency model and Spring
  Boot plugin configuration into `pom.xml`, removed Gradle project files, and
  updated documentation.
- AI result: edited
- Engineer changes and rationale: Maven is now the project build authority;
  documentation uses the installed-command form `mvn` because no Maven wrapper
  is checked in.
- Validation performed: POM and repository references inspected; whitespace
  validation completed. Maven/Java execution remains unavailable.
- Human sign-off: pending
