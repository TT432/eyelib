---
phase: 10-schema-runtime-ownership-adapter
verified: 2026-05-09T07:48:35Z
status: passed
score: 8/8 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: passed
  previous_score: 8/8
  gaps_closed: []
  gaps_remaining: []
  regressions: []
---

# Phase 10: Schema/Runtime Ownership & Adapter Verification Report

**Phase Goal:** Importer/raw particle schema and executable runtime particle definitions have explicit canonical owners and a tested conversion seam.
**Verified:** 2026-05-09T07:48:35Z
**Status:** passed
**Re-verification:** Yes — after code review fixes CR-01, WR-01, and WR-02

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Maintainer can identify the canonical owner for importer/raw particle schema and the canonical owner for runtime executable particle definitions. | ✓ VERIFIED | `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `eyelib-particle/.../README.md`, runtime `package-info.java`, and root particle README name importer `io.github.tt432.eyelibimporter.particle.BrParticle` as canonical raw schema/codec owner, particle `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` as canonical module runtime definition owner, and root `client/particle/bedrock/BrParticle` as legacy/non-canonical. `ParticleDefinitionDocumentationTest` locks this wording. |
| 2 | Runtime particle definitions can be created from importer/raw schema through a named adapter or equivalent explicit conversion seam. | ✓ VERIFIED | `ParticleDefinitionAdapter.fromSchema(BrParticle schema)` exists, imports importer `BrParticle`, returns `DataResult<ParticleDefinition>`, validates required schema nodes, and constructs `ParticleDefinition` from importer schema data. |
| 3 | Codec/schema behavior and runtime conversion expectations are covered by tests or documented invariants so duplicate `BrParticle` ownership cannot drift silently. | ✓ VERIFIED | `ParticleDefinitionAdapterTest` decodes fixture data through importer `BrParticle.CODEC`; `ParticleDefinitionBoundaryTest` scans all particle main sources for duplicate `record/class BrParticle` and forbidden root/MC/Forge references; `ParticleDefinitionDocumentationTest` locks ownership/mapped-field wording. Targeted JetBrains MCP test run passed. |
| 4 | Adapter preserves parity-critical particle fields needed by loading, rendering, Molang, lifetime, and remove behavior. | ✓ VERIFIED | `ParticleDefinition` stores `formatVersion`, `identifier`, render material/texture, `Map<String, BrParticle.Curve>`, `BrParticle.Events`, raw `BedrockResourceValue` components, and optional `BillboardFlipbook`. `ParticleDefinitionAdapterTest` asserts real fixture identifier/version/render params/component nested values/curve keys/flipbook values and, after CR-01, non-empty event data preservation. |
| 5 | Invalid or incomplete schema input fails loudly through `DataResult` instead of silently producing partial runtime definitions. | ✓ VERIFIED | `ParticleDefinitionAdapter.fromSchema` returns `DataResult.error` for null schema, null particle effect, null description, blank identifier, missing render parameters, blank material, and blank texture. `ParticleDefinitionAdapterTest` covers all of these WR-02 validation branches and asserts no partial result is produced. |
| 6 | `:eyelib-particle` remains root/MC/Forge-clean while intentionally allowing importer/Molang schema dependencies for the adapter seam. | ✓ VERIFIED | `eyelib-particle/build.gradle` intentionally depends on `:eyelib-importer` and `:eyelib-molang`, with no root project dependency. IDE regex import scan found no forbidden root/MC/Forge imports under `eyelib-particle/src/main/java`; `ParticleDefinitionBoundaryTest` also strips comments/string literals and scans for forbidden references beyond imports after WR-01. |
| 7 | No duplicate particle-module `BrParticle` type exists. | ✓ VERIFIED | IDE text scans under `eyelib-particle/src/main/java` found no `record BrParticle` or `class BrParticle`; `ParticleDefinitionBoundaryTest` enforces the same source-wide. |
| 8 | Phase 10 scope does not silently move deferred runtime/loading/command/network behavior. | ✓ VERIFIED | Roadmap/docs explicitly defer executable runtime core to Phase 11, loading/publication to Phase 12, command/network integration to Phase 13, and final verification to Phase 14. Verified Phase 10 artifacts are schema/runtime-definition adapter, documentation, and boundary/parity tests only. |

**Score:** 8/8 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
| -------- | -------- | ------ | ------- |
| `eyelib-particle/build.gradle` | Intentional particle -> importer/Molang dependency for adapter seam | ✓ VERIFIED | Contains `implementation project(':eyelib-importer')` and `implementation project(':eyelib-molang')`; no root project dependency added. |
| `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` | Canonical raw Bedrock particle schema/codec owner retaining event payloads | ✓ VERIFIED | `BrParticle.Events` now stores `Map<String, BedrockResourceValue> values`, copies it as an unmodifiable linked map, and `Events.CODEC` decodes raw event values instead of dropping them. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java` | Canonical module runtime particle definition with distinct non-`BrParticle` name | ✓ VERIFIED | Record with immutable copied maps and fields for version, identifier, render params, curves, events, raw components, and flipbook summary. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java` | Named importer schema to runtime definition conversion seam | ✓ VERIFIED | `fromSchema(BrParticle schema)` returns `DataResult<ParticleDefinition>`, validates required data, preserves events/raw components/curves/flipbook, and does not import root/MC/Forge packages. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java` | Runtime package ownership and boundary invariants | ✓ VERIFIED | Documents canonical owners, root legacy status, allowed adapter dependency, mapped fields, and Phase 11/12/13 deferrals. |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java` | Real-fixture parity, event preservation, and loud-failure coverage | ✓ VERIFIED | Uses copied `witchspell.json`, importer `BrParticle.CODEC`, adapter conversion, parity assertions, non-empty event payload assertion, and full validation error branch assertions. |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java` | Forbidden reference and no duplicate `BrParticle` drift checks | ✓ VERIFIED | Walks `eyelib-particle/src/main/java`, rejects duplicate `BrParticle` declarations, strips comments/string literals, and rejects forbidden root/MC/Forge reference fragments beyond imports. |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionDocumentationTest.java` | Documentation invariant checks | ✓ VERIFIED | Reads module/architecture/README/package docs and asserts owner, legacy, mapped-field, and deferral wording. |
| `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, particle READMEs | Ownership docs updated | ✓ VERIFIED | Direct reads show Phase 10 canonical owner, adapter, root legacy, mapped-field, and deferral statements. |

### Key Link Verification

| From | To | Via | Status | Details |
| ---- | --- | --- | ------ | ------- |
| `ParticleDefinitionAdapter.java` | importer `BrParticle.java` | `fromSchema(BrParticle schema)` | ✓ WIRED | Adapter imports `io.github.tt432.eyelibimporter.particle.BrParticle` and accepts it as the schema input. |
| importer `BrParticle.Events.CODEC` | `ParticleDefinition.events()` | raw event value decode then adapter copy | ✓ WIRED | `BrParticle.Events` decodes event map values as `BedrockResourceValue`; adapter passes `effect.events()` into `ParticleDefinition`; `eventfulFixturePreservesRawEventDataThroughAdapter` asserts non-empty `particle_expired.sequence[0].event`. |
| `ParticleDefinitionAdapterTest.java` | `witchspell.json` fixture | `BrParticle.CODEC` decode then adapt | ✓ WIRED | Test loads classpath fixture and parses via importer `BrParticle.CODEC.parse(JsonOps.INSTANCE, ...)` before calling adapter. |
| `MODULES.md` / architecture docs | `ParticleDefinition.java` / adapter seam | named owner documentation | ✓ WIRED | Docs explicitly reference `ParticleDefinition`, `ParticleDefinitionAdapter`, importer `BrParticle`, and legacy root `BrParticle`. |
| `ParticleDefinitionBoundaryTest.java` | `eyelib-particle/src/main/java` | `Files.walk` source scan | ✓ WIRED | Test scans all main Java sources, strips comments/string literals for forbidden-reference checks, and covers duplicate `BrParticle` declarations. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| -------- | ------------- | ------ | ------------------ | ------ |
| `BrParticle.Events` | `Map<String, BedrockResourceValue> values` | importer `Events.CODEC` over JSON `events` object | Yes — decodes event payload objects into raw `BedrockResourceValue` map and preserves it | ✓ FLOWING |
| `ParticleDefinitionAdapter` | `ParticleDefinition` fields | importer `BrParticle` decoded from real `witchspell.json` plus inline eventful fixture | Yes — adapter copies schema data, including curves/events/raw components/flipbook, into runtime definition | ✓ FLOWING |
| `ParticleDefinitionAdapterTest` | fixture schema | classpath `witchspell.json` and inline non-empty events fixture | Yes — non-empty particle JSON and non-empty event payload are decoded through importer codec and asserted after adaptation | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| -------- | ------- | ------ | ------ |
| Full Phase 10 compile/test gate | JetBrains MCP `:eyelib-particle:test :eyelib-particle:compileJava :eyelib-importer:compileJava :compileJava` | exitCode 0, BUILD SUCCESSFUL | ✓ PASS |
| Targeted adapter/boundary/documentation tests | JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapterTest --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionBoundaryTest --tests io.github.tt432.eyelibparticle.runtime.ParticleDefinitionDocumentationTest` | exitCode 0, BUILD SUCCESSFUL | ✓ PASS |
| Duplicate `BrParticle` source scan | IDE text scan under `eyelib-particle/src/main/java` for `record BrParticle` and `class BrParticle` | no matches | ✓ PASS |
| Forbidden import source scan | IDE regex scan under `eyelib-particle/src/main/java` for forbidden root/MC/Forge imports | no matches | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| ----------- | ----------- | ----------- | ------ | -------- |
| PSCHEMA-01 | 10-01, 10-02 | Maintainer can identify canonical owner for importer/raw particle schema and executable runtime particle definitions. | ✓ SATISFIED | Ownership docs and `ParticleDefinitionDocumentationTest` cover importer `BrParticle`, particle `ParticleDefinition`, adapter, and root legacy status. |
| PSCHEMA-02 | 10-01, 10-02 | Runtime particle definitions are created from importer/raw schema through a named adapter with parity coverage. | ✓ SATISFIED | `ParticleDefinitionAdapter.fromSchema(BrParticle)` plus real-fixture and eventful adapter tests. |
| PSCHEMA-03 | 10-01, 10-02 | Duplicate `BrParticle` ownership cannot drift silently due to tests or documented invariants. | ✓ SATISFIED | Distinct `ParticleDefinition`; no duplicate `BrParticle` in particle module; boundary and documentation tests enforce no drift. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| ---- | ---- | ------- | -------- | ------ |
| — | — | None blocking in Phase 10 runtime/test/doc artifacts | — | No TODO/FIXME/placeholders, empty adapter implementation, missing event preservation, or root/MC/Forge contamination found in verified Phase 10 artifacts. Null checks in adapter are validation behavior, not stubs. |

### Human Verification Required

None. The Phase 10 outcome is code/docs/test boundary behavior and was fully verified by source inspection plus JetBrains MCP compile/test checks. No visual, real-time, external-service, hardware, or subjective UX behavior is part of this phase goal.

### Gaps Summary

No blocking gaps found. Code review fixes were verified in code rather than accepted from summaries:

- CR-01 is closed: importer `BrParticle.Events` preserves raw event payload data and adapter tests assert non-empty event data survives conversion.
- WR-01 is closed: boundary tests scan all particle main sources for forbidden root/MC/Forge references beyond imports while stripping comments/string literals.
- WR-02 is closed: adapter tests cover null particle effect, null description, blank material, and blank texture validation branches in addition to null schema, blank identifier, and missing render parameters.

Deferred later-phase work is explicitly documented in the roadmap and docs: Phase 11 runtime extraction, Phase 12 loading/publication rewire, Phase 13 command/network integration, and Phase 14 final verification/documentation gate. These are not Phase 10 gaps.

---

_Verified: 2026-05-09T07:48:35Z_
_Verifier: the agent (gsd-verifier)_
