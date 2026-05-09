---
phase: 12-loading-publication-rewire
fixed_at: 2026-05-09T00:00:00Z
review_path: .planning/phases/12-loading-publication-rewire/12-REVIEW.md
iteration: 1
findings_in_scope: 4
fixed: 4
skipped: 0
status: all_fixed
---

# Phase 12: Code Review Fix Report

**Fixed at:** 2026-05-09T00:00:00Z
**Source review:** `.planning/phases/12-loading-publication-rewire/12-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope: 4
- Fixed: 4
- Skipped: 0

## Fixed Issues

### CR-01: Add-on folder loading discards particle definitions instead of publishing them

**Files modified:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublication.java`, `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java`, `src/test/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlannerAddonBridgeTest.java`
**Commit:** `447d7a5`
**Applied fix:** Added schema-based replacement publication and routed Bedrock add-on particle aggregates through the module-owned active registry instead of clearing with an empty JSON map. Status: fixed; requires human verification for runtime behavior.

### CR-02: Root reload publication leaves existing animation particle-effect lookups empty

**Files modified:** `src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java`, `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java`, `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntryDefinition.java`, `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/controller/BrControllerExecutor.java`, `src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java`
**Commits:** `d1fb60f`, `ac8b1e3`, `6a9828e`
**Applied fix:** Added module-definition lookup/spawn overloads and migrated animation/controller particle effects to resolve `ParticleDefinitionRegistry` entries populated by reload/publication. Status: fixed; requires human verification for runtime behavior.

### WR-01: Add-on bridge tests assert the old empty-particle behavior and miss active registry publication

**Files modified:** `src/test/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlannerAddonBridgeTest.java`
**Commit:** `447d7a5`
**Applied fix:** Added an add-on particle fixture and assertions against `ParticleDefinitionRegistry` publication by description identifier, while preserving the legacy compatibility map empty assertion.

### WR-02: Root publication tests do not cover remaining runtime particle-effect resolution

**Files modified:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java`
**Commits:** `d1fb60f`, `6a9828e`
**Applied fix:** Added coverage that published module definitions resolve through `ParticleLookup.definition(...)` and that animation/controller particle-effect paths no longer call legacy `ParticleLookup.get(...)`.

## Verification

- JetBrains MCP `:test --tests io.github.tt432.eyelib.client.gui.manager.reload.ManagerResourceImportPlannerAddonBridgeTest --tests io.github.tt432.eyelib.client.particle.ParticleRuntimeDelegationBoundaryTest` exited 0 (External task id 10).
- JetBrains MCP `:eyelib-particle:test --tests io.github.tt432.eyelibparticle.loading.ParticleResourcePublicationTest` exited 0 (External task id 11).
- JetBrains MCP `:compileJava :eyelib-particle:compileJava` exited 0 (External task id 12).
- Earlier combined root/module filtered verification attempt failed before final fixes; final valid split runs above passed.

## Skipped Issues

None.

---

_Fixed: 2026-05-09T00:00:00Z_
_Fixer: gsd-code-fixer_
_Iteration: 1_
