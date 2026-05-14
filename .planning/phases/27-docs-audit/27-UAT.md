---
status: testing
phase: 27-docs-audit
source:
  - .planning/phases/27-docs-audit/27-CONTEXT.md
  - .planning/phases/27-docs-audit/27-PLAN.md
  - .planning/phases/27-docs-audit/27-VERIFICATION.md
  - .planning/phases/27-docs-audit/27-VALIDATION.md
started: 2026-05-13T20:20:12.2304916+08:00
updated: 2026-05-13T20:20:12.2304916+08:00
---

## Current Test

number: 1
name: Old Module Name Cleanup
expected: |
  Searching current production Java sources and current-state docs for the old module name `eyelib-processor` should not show active references. Historical migration notes may still mention it, but user-facing/current architecture docs should use `eyelib-preprocessing`.
awaiting: user response

## Tests

### 1. Old Module Name Cleanup
expected: Searching current production Java sources and current-state docs for the old module name `eyelib-processor` should not show active references. Historical migration notes may still mention it, but user-facing/current architecture docs should use `eyelib-preprocessing`.
result: [pending]

### 2. README Audit Coverage
expected: The documented README audit should cover all tracked README.md files for this repository, and there should be no leftover README.md in empty or invalid directories.
result: [pending]

### 3. Missing Module Documentation
expected: The previously missing `eyelib-material` module documentation should exist at `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/README.md` and describe scope, responsibilities, dependency direction, and editing rules.
result: [pending]

### 4. Module Topology Documentation
expected: `MODULES.md`, `docs/index/util.md`, `docs/index/repo-map.md`, and the main `docs/architecture/*.md` files should reflect the v1.5 module topology, including `eyelib-preprocessing` and the updated `eyelib-material` README reference.
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps

[none yet]
