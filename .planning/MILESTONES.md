# Milestones

## v1.2 真正实现 eyelib-particle 的模块分离 (Shipped: 2026-05-09)

**Phases completed:** 7 phases, 22 plans, 18 requirements

**Key accomplishments:**

- Promoted particle ownership into first-class `:eyelib-particle` Gradle module boundaries with documented one-way root-to-particle dependency direction.
- Established narrow particle module APIs for lookup, spawn/remove, store/publication, initialization, and transitional root compatibility facades.
- Split raw importer particle schema from executable runtime particle definitions through explicit `ParticleDefinition` and adapter ownership.
- Moved particle runtime/client/render behavior, loading/publication, command integration, and string-keyed network packet behavior behind explicit module or root/MC adapter seams.
- Closed the milestone with final boundary, parity, documentation, and JetBrains MCP verification evidence while documenting non-blocking future packaging/manual visual proof debt.

**Archive:** `.planning/milestones/v1.2-ROADMAP.md`, `.planning/milestones/v1.2-REQUIREMENTS.md`, `.planning/milestones/v1.2-MILESTONE-AUDIT.md`

---

## v1.1 ClientSmoke 全自动化 (Shipped: 2026-05-08)

**Phases completed:** 3 phases, 5 plans, 8 tasks

**Key accomplishments:**

- MDGL `clientSmoke` run config with isolated `run/clientsmoke/` game directory, unconditional `eyelib-clientsmoke` localRuntime dependency, and `.gitignore` entries for smoke test artifacts
- System-property-first config override methods in ClientSmokeConfig with Gradle `systemProperty()` JVM flag injection for smoke test auto-enable and auto-exit
- State machine fully wired to system-property-first config bridge — empty test sets generate report and exit, JUnit XML written alongside JSON, exit code signals pass/fail to Gradle
- Two JUnit Jupiter test classes with 21 tests statically verifying CORR-03 system property bridge behavior and build.gradle run config isolation — zero production code changes.

---
