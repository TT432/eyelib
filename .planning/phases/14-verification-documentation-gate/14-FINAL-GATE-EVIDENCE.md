# Phase 14 Final Gate Evidence

## Phase 8-13 Evidence Summary

| Phase | Status | Evidence Source | Verified Truths Carried Into Phase 14 |
|-------|--------|-----------------|---------------------------------------|
| Phase 8 | Passed | `.planning/phases/08-boundary-contract-gradle-module-skeleton/08-VERIFICATION.md` | `:eyelib-particle` is a first-class Gradle subproject; root consumes it one-way; module docs state responsibility and JetBrains MCP-only verification. |
| Phase 9 | Passed | `.planning/phases/09-particle-api-store-seam/09-VERIFICATION.md` | Root lookup, store/publication, lifecycle, and spawn/remove entrypoints pass through narrow `io.github.tt432.eyelibparticle.api` seams; root facades are transitional. |
| Phase 10 | Passed | `.planning/phases/10-schema-runtime-ownership-adapter/10-VERIFICATION.md` | Importer owns raw `io.github.tt432.eyelibimporter.particle.BrParticle`; `:eyelib-particle` owns `ParticleDefinition`; `ParticleDefinitionAdapter` preserves parity-critical fields and fails loudly. |
| Phase 11 | Passed | `.planning/phases/11-runtime-client-core-extraction/11-VERIFICATION.md` | Executable runtime, component dispatch, lifecycle, render manager, client hooks, and root delegation live behind module/runtime/client boundaries; visual proof deferred to Phase 14. |
| Phase 12 | Passed | `.planning/phases/12-loading-publication-rewire/12-VERIFICATION.md` | `ParticleDefinitionRegistry` and `ParticleResourcePublication` own active loading/publication; active keys are `ParticleDefinition.identifier()` and source `ResourceLocation` values are diagnostics metadata only. |
| Phase 13 | Passed | `.planning/phases/13-command-network-integration-rewire/13-VERIFICATION.md`, `.planning/phases/13-command-network-integration-rewire/13-REVIEW.md` | `/eyelib particle` compatibility, string-keyed spawn/remove packets under `mc/impl/network/packet`, `mc/impl/common/command` adapter ownership, and `NetClientHandlers` delegation are verified; review is clean. |

## PVERIFY-01 Evidence

PVERIFY-01 is finalized in later Phase 14 plans. Evidence categories to fill:

| Evidence Category | Required Artifact / Task | Plan 03 Result Placeholder |
|-------------------|--------------------------|----------------------------|
| Existing particle assertions not weakened | Targeted root and `:eyelib-particle` JUnit tests adapted in Plan 02. | ⬜ Fill exact JetBrains MCP task names and exit codes in Plan 03. |
| Final documentation drift guard | Stable-doc tests must read repository docs only, not `.planning/` files. | ⬜ Fill test class names and results in Plan 03. |
| Boundary/parity/regression coverage | Dependency direction, schema/runtime conversion, reload keys, command/network delegation, side boundaries. | ⬜ Fill targeted matrix row in Plan 03. |

## PVERIFY-02 Evidence

PVERIFY-02 is finalized by JetBrains MCP Gradle checks plus separate ClientSmoke/manual evidence.

| Evidence Category | Required Artifact / Task | Plan 03 Result Placeholder |
|-------------------|--------------------------|----------------------------|
| JetBrains MCP Gradle matrix | `:eyelib-particle:test`, `:eyelib-particle:compileJava`, `:compileJava`, and targeted root `:test` filters. | ⬜ Fill exact `jetbrain_run_gradle_tasks` task names, script parameters, exitCode, and status in Plan 03. |
| ClientSmoke applicability | Existing ClientSmoke hook status recorded without creating a new smoke framework. | ⬜ Fill applicability decision in Plan 03. |
| Hardware/manual checklist | `14-HARDWARE-CHECKLIST.md` records manual visual checks and Windows hardware exit-code capture as manual/deferred per D-15. | ⬜ Fill operator/environment/evidence rows if run. |
| Closure rationale | Milestone closure must cite requirements status, explicit deferrals, residual risks, and non-blocking rationale. | ⬜ Fill after Plan 03 matrix and checklist status. |

## JetBrains MCP Matrix Results

Plan 03 must replace the placeholders below with exact task names, script parameters, external task ids if available, exit codes, and pass/fail/triage notes. Do not run Gradle through shell.

| Matrix Row | Required JetBrains MCP Task | Result |
|------------|-----------------------------|--------|
| Module compile/test gate | `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]` | ⬜ Pending Plan 03 |
| Particle module final tests | `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test"]` with final boundary/parity filters | ⬜ Pending Plan 03 |
| Root final split tests | `jetbrain_run_gradle_tasks` taskNames=`[":test"]` with documentation/root adapter/command/network filters | ⬜ Pending Plan 03 |
| Optional broad root `:test` | Only if local runtime budget permits; classify failures per D-03/D-11. | ⬜ Pending Plan 03 decision |

## ClientSmoke And Hardware Status

Manual and hardware evidence is separate from automated Gradle gates. See `14-HARDWARE-CHECKLIST.md` for the checklist and result log.

| Category | Status | Notes |
|----------|--------|-------|
| ClientSmoke applicability | ⬜ Pending Plan 03 | Use existing hooks only; do not create broad new smoke framework work. |
| Manual visual checks | ⬜ Pending / manual | Records real client particle behavior if available. |
| Windows hardware exit-code capture | Manual/deferred | D-15 says this is not a mandatory automated v1.2 gate. |

## Residual Risks

| Risk | Treatment |
|------|-----------|
| PFUT-02 packet-contract relocation remains open | Future requirement; not a v1.2 blocker because current root/MC packet ownership is documented and tested. |
| PFUT-03 independent particle artifact publication remains open | Future packaging requirement; not needed to prove the in-repo module boundary. |
| Unrelated full-suite fixture failures | Triage as residual unless they prevent particle-gate targeted tests from running. |
| Manual visual proof may be unavailable in automation-only sessions | Keep as hardware/manual evidence instead of automated Gradle proof. |

## Milestone Closure Rationale

Closure can be claimed only after Plan 03 fills the JetBrains MCP matrix, ClientSmoke/manual status, and requirement status. The intended closure rationale is:

1. Phase 8-13 verified the module split foundations, API/store seam, schema/runtime owner split, runtime/client extraction, loading/publication rewire, and command/network adapter boundary.
2. Phase 14 Plan 01 aligns stable documentation and creates the evidence shells.
3. Phase 14 Plan 02/03 must provide final tests and exact JetBrains MCP results without weakening prior assertions.
4. PFUT-02, PFUT-03, unrelated fixture cleanup, Windows hardware exit-code capture, and purely manual visual proof are explicitly non-blocking or future/manual evidence items.
