---
phase: 10-schema-runtime-ownership-adapter
fixed_at: 2026-05-09T07:33:11Z
review_path: .planning/phases/10-schema-runtime-ownership-adapter/10-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 10: Code Review Fix Report

**Fixed at:** 2026-05-09T07:33:11Z
**Source review:** `.planning/phases/10-schema-runtime-ownership-adapter/10-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 3
- Fixed: 3
- Skipped: 0

## Fixed Issues

### CR-01: Bedrock particle event contents are silently dropped

**Files modified:** `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java`, `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java`
**Commit:** 6e0cbf5
**Applied fix:** Changed importer `BrParticle.Events` to retain raw event values and added adapter coverage for non-empty event data.

### WR-01: Boundary test can be bypassed with fully-qualified forbidden references

**Files modified:** `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java`
**Commit:** 840c61b, 0671f05
**Applied fix:** Expanded the boundary scan to detect forbidden package references beyond imports, then stripped comments/string literals so documentation mentions do not trigger false positives.

### WR-02: Adapter validation paths documented in the summary are not covered by tests

**Files modified:** `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java`
**Commit:** 8f669c0
**Applied fix:** Added tests for null particle effect, null description, blank material, and blank texture validation errors.

---

_Fixed: 2026-05-09T07:33:11Z_
_Fixer: the agent (gsd-code-fixer)_
_Iteration: 1_
