# Phase 14: Verification & Documentation Gate - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 14 is the final v1.2 verification and documentation gate for the `:eyelib-particle` module split. It must prove that prior Phase 8-13 work preserved particle behavior and left repository documentation consistent with the final ownership boundaries. This phase owns test relocation/adaptation, broad boundary/parity/regression coverage, JetBrains MCP-only Gradle verification, applicable automated ClientSmoke evidence, explicit hardware/manual deferrals for runtime behavior that cannot be automatically asserted, and milestone completion evidence. It must not introduce new particle runtime behavior or move source ownership unless a verification or documentation gap directly requires the smallest corrective change.

</domain>

<decisions>
## Implementation Decisions

### Final Test Coverage
- **D-01:** Phase 14 must treat PVERIFY-01 as a no-weakening gate. Existing particle-related tests may be moved, renamed, or adapted only if their assertions remain at least as strong and still prove the same observable behavior after the module split.
- **D-02:** New or updated tests should cover the complete split, not just the latest command/network work: root -> particle dependency direction, forbidden reverse/root/MC/Forge imports in pure module areas, importer schema to `ParticleDefinition` parity, loading publication keys, root compatibility adapter delegation, command/network packet delegation, client side boundaries, and documentation drift.
- **D-03:** Broad root test-suite cleanup is in scope only where it relates to stale particle invariants or particle split regressions. Unrelated fixture failures should be recorded as residual/non-blocking unless they prevent the planned particle gate from running.
- **D-04:** Prefer the repository's existing JUnit 5 style: flat `*Test.java` classes, descriptive package-private test methods, real codecs/fixtures for parity checks, hand-written doubles, and source-scan boundary tests for import/delegation invariants.

### Documentation Consistency
- **D-05:** Documentation must converge on the final ownership story already proven by Phases 8-13: `:eyelib-particle` owns module APIs, runtime definition, adapter, executable runtime, client integration, render manager, and loading/publication; root owns Forge/resource, command, network, and compatibility adapters; importer owns raw `BrParticle` schema.
- **D-06:** The final documentation gate must check and update, if needed, `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md`, `src/main/java/io/github/tt432/eyelib/client/particle/README.md`, and `src/main/java/io/github/tt432/eyelib/network/README.md` so they agree with code and tests.
- **D-07:** `.planning/` artifacts may provide planning evidence, but normal source tests must not depend on `.planning/` files. Phase 13 review explicitly closed that issue; Phase 14 should preserve that rule for any documentation drift tests.
- **D-08:** Docs must keep future work separate from final v1.2 completion: PFUT-02 packet-contract relocation, PFUT-03 independent artifact publication, unrelated full-suite fixture cleanup, and purely manual visual proof are not blockers unless the roadmap or requirements say otherwise.

### JetBrains MCP Verification Matrix
- **D-09:** All Gradle verification must be run through JetBrains MCP Gradle tools only. Do not run shell Gradle commands.
- **D-10:** The planned automated matrix should include at minimum: `:eyelib-particle:test`, `:eyelib-particle:compileJava`, `:compileJava`, and targeted root `:test` filters for particle API/store, schema adapter, runtime/client integration, loading/publication, command/network, documentation drift, and boundary tests.
- **D-11:** If the planner chooses to run a broad root `:test`, failures must be triaged into particle-gate regressions versus unrelated existing fixture failures. Only particle-gate regressions block Phase 14 completion.
- **D-12:** Null-safety verification is required only if Phase 14 changes null-safety-sensitive code or source annotations. If only tests/docs are edited, do not add unnecessary NullAway scope.

### ClientSmoke And Hardware Evidence
- **D-13:** Automated ClientSmoke is applicable only where existing ClientSmoke hooks can provide meaningful particle-module confidence without broad new smoke framework work. Phase 14 may add or update smoke coverage only if it fits the current client-smoke flow and does not become a new runtime feature project.
- **D-14:** Hardware/manual visual checks remain separate from automated Gradle gates. They should be captured as a checklist/evidence item for behavior that cannot be automatically asserted, especially real Minecraft rendering/visual behavior deferred from Phases 11 and 13.
- **D-15:** Windows hardware exit-code capture remains deferred/manual per the project-level out-of-scope note. Do not make it a mandatory automated gate for v1.2 completion.

### Milestone Completion Evidence
- **D-16:** Phase 14 should produce evidence that PVERIFY-01 and PVERIFY-02 are satisfied and that all v1.2 requirements either are complete or are explicitly deferred as future requirements/out-of-scope items.
- **D-17:** The final gate should summarize Phase 8-13 verified truths and review status rather than re-litigating already passed phase decisions. Use prior verification files as evidence and fill only remaining final-gate gaps.
- **D-18:** Completion evidence should be maintainer-oriented: list exact JetBrains MCP task names/results, targeted test groups, documentation files checked, ClientSmoke/hardware status, residual risks, and the rationale for milestone closure.

### Claude's Discretion
- No user-only grey area remains. Planner/executor may choose the smallest verification/documentation plan that satisfies PVERIFY-01/PVERIFY-02, preserves existing behavior, avoids source churn, and produces clear final gate evidence.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project And Phase Scope
- `.planning/PROJECT.md` - v1.2 milestone goal, zero-regression particle split requirement, active/out-of-scope requirements, ClientSmoke/hardware deferrals, and JetBrains MCP-only Gradle rule.
- `.planning/REQUIREMENTS.md` - PVERIFY-01/PVERIFY-02 final gate requirements plus all completed v1.2 requirement mappings and future deferrals.
- `.planning/ROADMAP.md` - Phase 14 goal, dependency on Phase 13, success criteria, and final v1.2 phase status.
- `.planning/STATE.md` - current Phase 14 position, accumulated Phase 8-13 decisions, no pending todos/blockers, and Phase 13 completion state.

### Prior Phase Verification Evidence
- `.planning/phases/08-boundary-contract-gradle-module-skeleton/08-VERIFICATION.md` - verified Gradle module skeleton, one-way root -> particle dependency, module docs, and MCP compile evidence.
- `.planning/phases/09-particle-api-store-seam/09-VERIFICATION.md` - verified API/store seams, transitional root facades, string-keyed requests, publication identifiers, and targeted MCP tests.
- `.planning/phases/10-schema-runtime-ownership-adapter/10-VERIFICATION.md` - verified importer raw schema owner, particle runtime definition owner, adapter parity, no duplicate `BrParticle`, and boundary tests.
- `.planning/phases/11-runtime-client-core-extraction/11-VERIFICATION.md` - verified executable runtime/client integration extraction, side-safe client hooks, root delegation, and Phase 14 deferrals for visual/client evidence and broad root test cleanup.
- `.planning/phases/12-loading-publication-rewire/12-VERIFICATION.md` - verified resource reload/publication rewire, description-identifier active keys, root compatibility adapters, add-on publication, and MCP test evidence.
- `.planning/phases/13-command-network-integration-rewire/13-VERIFICATION.md` - verified command compatibility, string-keyed spawn/remove packets, handler delegation, adapter ownership, and broad ClientSmoke/hardware deferral.
- `.planning/phases/13-command-network-integration-rewire/13-REVIEW.md` - clean Phase 13 review status after fixes; confirms documentation tests no longer depend on `.planning/` artifacts and packet tests perform real codec round trips.

### Prior Phase Context
- `.planning/phases/11-runtime-client-core-extraction/11-CONTEXT.md` - runtime extraction decisions and explicit Phase 14 deferrals.
- `.planning/phases/12-loading-publication-rewire/12-CONTEXT.md` - loading/publication verification expectations and Phase 14 broad gate deferrals.
- `.planning/phases/13-command-network-integration-rewire/13-CONTEXT.md` - command/network compatibility decisions, verification expectations, and final gate handoff.

### Repository Boundary Rules
- `AGENTS.md` - repository reading, editing, Gradle, module update, and verification rules.
- `MODULES.md` - canonical module inventory, particle subproject row, root compatibility adapters, command/network ownership, and module update rules.
- `docs/index/repo-map.md` - repository navigation, particle module route, sync/packet route, and Phase 14 evidence note.
- `docs/architecture/01-module-boundaries.md` - final module ownership map, particle loading/publication notes, command/network notes, and root adapter policy.
- `docs/architecture/02-side-boundaries.md` - side rules, pure particle cleanliness, string/ResourceLocation boundary, and Phase 14 broad verification note.

### Package Documentation
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` - particle module responsibilities, dependency direction, integration rule, current consumers, Phase 14 evidence ownership, and MCP verification rule.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` - retained root particle adapter boundaries, active lookup/spawn rules, packet/runtime adaptation, and Phase 14 broad evidence deferral.
- `src/main/java/io/github/tt432/eyelib/network/README.md` - network package scope, packet DTO/codec ownership under `mc/impl/network/packet`, `NetClientHandlers` delegation rule, and MCP-only verification.

### Codebase Scout Maps
- `.planning/codebase/TESTING.md` - JUnit 5 conventions, fixture patterns, boundary/static test style, targeted Gradle task examples, and no configured coverage threshold.
- `.planning/codebase/CONVENTIONS.md` - naming, formatting, null-safety, logging, comments, and README/package documentation conventions.
- `.planning/codebase/STRUCTURE.md` - module/subproject layout, test locations, `mc/impl` quarantine zone, and current package navigation.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Phase 8-13 verification files already provide passed, source-backed evidence for module skeleton, API/store, schema adapter, runtime/client integration, loading/publication, and command/network behavior.
- Existing particle tests already cover `ParticlePublisher`, store/order behavior, adapter parity, boundary scans, runtime components/lifecycle, render manager lifecycle, loading/publication, command runtime, packet codecs, handler delegation, and documentation drift. Phase 14 should consolidate/adapt this coverage instead of replacing it with weaker checks.
- `:eyelib-particle` is the primary subproject for particle module tests; root `:test` remains needed for adapter, command/network, and documentation drift tests that live in root packages.
- ClientSmoke exists as an external composite-build framework, but prior phase evidence intentionally deferred broad/client visual proof to Phase 14 rather than treating it as a blocker for phases 11-13.

### Established Patterns
- Gradle tasks are invoked through JetBrains MCP only.
- Boundary correctness is enforced through a combination of source scans and behavioral JUnit assertions.
- Repository docs must be stable source-controlled docs; normal tests should not read `.planning/` phase artifacts.
- Root compatibility adapters are allowed only when named, documented, and delegating to module APIs/services; they must not regain canonical particle ownership.
- Manual/hardware checks are recorded separately from automated test gates.

### Integration Points
- Automated verification connects through JetBrains MCP `jetbrain_run_gradle_tasks` with targeted Gradle task names and `--tests` filters.
- Documentation consistency connects through `MODULES.md`, repo map, architecture docs, particle README, root particle README, and network README.
- Final behavior evidence connects the Phase 11 runtime/client extraction, Phase 12 loading/publication rewire, and Phase 13 command/network rewire into a single milestone closure report.

</code_context>

<specifics>
## Specific Ideas

The final gate should be evidence-first: verify the existing module split, adapt stale tests without weakening them, run a clear JetBrains MCP matrix, update only inconsistent docs, record ClientSmoke/hardware status separately, and produce a maintainer-readable closure artifact for v1.2.

</specifics>

<deferred>
## Deferred Ideas

- Permanent packet-contract relocation remains PFUT-02 and is not part of Phase 14 unless documentation must mention the deferral.
- Independent `:eyelib-particle` external artifact publication remains PFUT-03/future packaging scope.
- Windows hardware exit-code capture remains manual/deferred per project scope; record any checklist outcome separately from automated gates.
- Unrelated broad root test-suite fixture failures are not Phase 14 blockers unless they mask or cause particle split regressions.

</deferred>

---

*Phase: 14-Verification & Documentation Gate*
*Context gathered: 2026-05-09*
