# Milestones

## v1.1 ClientSmoke 全自动化 (Shipped: 2026-05-08)

**Phases completed:** 3 phases, 5 plans, 8 tasks

**Key accomplishments:**

- MDGL `clientSmoke` run config with isolated `run/clientsmoke/` game directory, unconditional `eyelib-clientsmoke` localRuntime dependency, and `.gitignore` entries for smoke test artifacts
- System-property-first config override methods in ClientSmokeConfig with Gradle `systemProperty()` JVM flag injection for smoke test auto-enable and auto-exit
- State machine fully wired to system-property-first config bridge — empty test sets generate report and exit, JUnit XML written alongside JSON, exit code signals pass/fail to Gradle
- Two JUnit Jupiter test classes with 21 tests statically verifying CORR-03 system property bridge behavior and build.gradle run config isolation — zero production code changes.

---
