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
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

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
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

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
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

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
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

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
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-21 — SCENARIO-B — Retention, redaction, and bulk export hardening
- Intent: Make the Scenario B implementation explicit, reusable for archival
  retrieval, and safe to verify without retaining archived payloads.
- Constraints and technical context: Preserve legacy chain verification, avoid
  raw sensitive payload text in new hash material and archive metadata, and
  provide honest export completeness boundaries.
- Prompt/approach summary: AI reviewed the existing archive/redaction/export
  implementation, added migration V3, versioned payload-digest hashing,
  payload-digest verification, and `SCENARIO_B.md`.
- AI result: edited
- Engineer changes and rationale: New records use
  `SHA-256-PAYLOAD-DIGEST-V2`; legacy records retain their original algorithm
  identifier so historical data is not silently reinterpreted.
- Validation performed: Source, SQL column/value arity, and documentation
  inspection completed. Maven tests remain blocked because Java and Maven are
  unavailable in the execution environment.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — SCENARIO-A — Core audit service specification
- Intent: Document the core greenfield requirements and validation walkthrough
  as the contract shared by the implementation and Scenario B/C extensions.
- Constraints and technical context: Align event fields, timestamp behavior,
  append-only semantics, filtering, pagination, hash versions, archive-aware
  verification, and direct-database tamper detection.
- Prompt/approach summary: AI created `SCENARIO_A.md` and linked it from the
  README without changing API behavior.
- AI result: generated
- Engineer changes and rationale: The document distinguishes required core
  behavior, compatibility aliases, production assumptions, and deferred
  controls so Scenario B/C do not redefine the Scenario A contract.
- Validation performed: Documentation cross-referenced against the current
  controllers, hash implementation, migrations, tests, `SCENARIO_B.md`, and
  `SCENARIO_C.md`.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — ALIGNMENT — Cross-scenario review and trade-offs
- Intent: Check Scenarios A, B, and C against the requested requirements and
  make trade-offs, challenges, boundaries, and validation guardrails explicit.
- Constraints and technical context: Scenario C needed to address the
  ambiguous compliance-reporting requirement rather than duplicate Scenario B.
- Prompt/approach summary: AI reviewed all scenario documents and README,
  corrected Scenario C, added `TRADEOFFS_AND_CHALLENGES.md`, and cross-linked
  the shared reference.
- AI result: edited
- Engineer changes and rationale: The documents now share one event/hash/
  archival contract while distinguishing implemented assignment scope from
  production compliance controls.
- Validation performed: Cross-document consistency review and `git diff --check`
  completed. Automated tests remain unavailable because Java and Maven are not
  installed.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — SCENARIO-A-HLD-LLD — Core engineering specification
- Intent: Expand Scenario A from an API overview into a complete core-service
  HLD/LLD and engineering execution specification.
- Constraints and technical context: Align with the implemented Spring Boot,
  PostgreSQL, Flyway, versioned hash, archival, export, and verification model
  without claiming deferred production controls.
- Prompt/approach summary: AI updated `SCENARIO_A.md` with system boundaries,
  components, data flow, persistence, hash contract, transactions,
  verification, task decomposition, AI governance, quality gates, risks, and
  final-summary requirements.
- AI result: edited
- Engineer changes and rationale: The document now provides actionable
  dependency-ordered tasks and explicit human approval gates for high-impact
  integrity, schema, privacy, and deployment decisions.
- Validation performed: Cross-reference and source-alignment review completed;
  automated build/test execution remains blocked because Java and Maven are
  unavailable.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — CORE-HLD-LLD — Dedicated architecture reference
- Intent: Provide a standalone document showcasing the core requirements,
  high-level design, low-level design, execution plan, AI controls, quality
  gates, risks, and final engineering summary.
- Constraints and technical context: Keep the reference aligned with the
  implemented REST, PostgreSQL, Flyway, hash, archive, export, and verifier
  behavior.
- Prompt/approach summary: AI created `REQUIREMENTS_HLD_LLD.md` and
  cross-linked it from `SCENARIO_A.md` and `README.md`.
- AI result: generated
- Engineer changes and rationale: The architecture content is now available
  as a single reviewable artifact instead of being embedded only in Scenario A.
- Validation performed: Cross-document references and repository structure
  inspected; automated Maven tests remain blocked because Java and Maven are
  unavailable.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — CORE-NFR — Unified functional and non-functional specification
- Intent: Make the HLD/LLD document the single technical source for Scenarios
  A, B, and C, including senior-engineering non-functional requirements.
- Constraints and technical context: Keep scenario files concise and
  requirement-focused while centralizing implementation, performance,
  reliability, security, privacy, compatibility, and operational details.
- Prompt/approach summary: AI added cross-scenario functional/NFR sections to
  `REQUIREMENTS_HLD_LLD.md` and reduced the three scenario files to
  high-level scope, ambiguity, acceptance, and reference content.
- AI result: edited
- Engineer changes and rationale: Technical duplication was removed from the
  scenario summaries so hash, schema, API, quality, risk, and oversight
  decisions have one maintained source.
- Validation performed: Cross-document links, scenario coverage, and
  `git diff --check` reviewed. Automated Maven tests remain blocked because
  Java and Maven are unavailable.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — PENDING-TODOS — Kafka and operations hardening
- Intent: Add the two remaining pending engineering tasks to the maintained
  requirements and workflow documentation.
- Constraints and technical context: The task register identifies Kafka
  integration and production operations hardening as pending, with operations
  dependent on the integration and attestation work.
- Prompt/approach summary: AI queried the session task register, confirmed the
  two pending items, and added their intent, sequencing, acceptance criteria,
  risks, and quality expectations to `ENGINEERING_WORKFLOW.md` and
  `REQUIREMENTS_HLD_LLD.md`.
- AI result: edited
- Engineer changes and rationale: The pending scope is now visible in both the
  operational task workflow and the consolidated technical specification;
  neither task is represented as implemented.
- Validation performed: Pending-task query, documentation cross-reference
  review, and `git diff --check` completed. Automated build/test execution
  remains blocked because Java and Maven are unavailable.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — SUBMISSION-DOCS — Setup and final engineering summary
- Intent: Complete the repository submission documentation with runnable setup
  instructions and a final engineering summary covering artifacts, validation,
  risks, trade-offs, assumptions, and limitations.
- Constraints and technical context: Documentation must reflect the actual
  Java 21/Maven/PostgreSQL prototype, distinguish implemented behavior from
  deferred production controls, and avoid claiming tests that could not run.
- Prompt/approach summary: AI audited the existing Markdown set and added
  `SETUP.md` and `FINAL_ENGINEERING_SUMMARY.md`, expanded `ATTESTATION.md`, and
  linked the submission artifacts from `README.md`.
- AI result: edited
- Engineer changes and rationale: Setup examples use the existing REST routes,
  environment variables, Flyway behavior, H2 test profile, and honest Kafka
  boundary. The summary consolidates the existing design rather than
  introducing new implementation claims.
- Validation performed: Cross-reference review, repository status inspection,
  and `git diff --check`. Automated build/test execution remains blocked
  because Java and Maven are unavailable.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22

### 2026-09-22 — SETUP-KAFKA — Kafka delivery and consumer idempotency guidance
- Intent: Document at-most-once and at-least-once Kafka delivery choices and
  durable consumer-side idempotency handling in the local setup guide.
- Constraints and technical context: Kafka is disabled in the current
  prototype; documentation must not claim an implemented broker adapter or
  exactly-once behavior.
- Prompt/approach summary: AI updated `SETUP.md` with illustrative Spring
  Kafka settings, offset-commit timing, producer durability settings, and a
  durable inbox/deduplication flow keyed by immutable `eventId`.
- AI result: edited
- Engineer changes and rationale: At-least-once is recommended for audit
  events because loss is generally worse than replay. At-most-once is
  documented as an explicit loss-tolerant option. Consumer idempotency is
  required because Kafka delivery mode alone does not protect external side
  effects.
- Validation performed: Configuration and endpoint cross-reference review and
  `git diff --check`. Broker-backed tests remain pending because Kafka is not
  implemented or enabled in the prototype.
- Human sign-off: Sai Rama Rao Nayeni/ 2026-9-22
