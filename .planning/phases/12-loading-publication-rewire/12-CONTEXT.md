# Phase 12: Loading & Publication Rewire - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 12 moves particle resource reload, active registry replacement, and publication key semantics behind the `:eyelib-particle` module boundary without changing observable registry behavior. The phase must preserve the existing `particles/*.json` reload path, full-store replacement behavior, and publication by `particle_effect.description.identifier`, while leaving user-facing command/network behavior to Phase 13 and broad/client verification evidence to Phase 14.

</domain>

<decisions>
## Implementation Decisions

### Loader Ownership
- **D-01:** Particle-specific loading and publication ownership should move behind `:eyelib-particle`; root loader classes may remain only as explicit transitional adapters or Forge lifecycle registration shims.
- **D-02:** The resource discovery contract remains unchanged: Forge reload still scans the existing `eyelib/particles` JSON resource location through the current `particles/*.json` semantics. Do not rename resource folders, suffixes, or resource-pack expectations in this phase.
- **D-03:** Minecraft/Forge reload registration remains in a side-safe integration layer. If planner moves a concrete particle reload listener into `:eyelib-particle`, it must live in a documented client/integration package, not pure `api/**` or `runtime/**`.
- **D-04:** The loader should parse raw Bedrock particle JSON through the canonical importer schema and convert through `ParticleDefinitionAdapter` where the active module registry needs runtime definitions. Do not treat root `client/particle/bedrock/BrParticle` as the canonical schema owner again.

### Publication Semantics
- **D-05:** Replacement publication must continue to key entries by `particle_effect.description.identifier`, represented by `ParticleDefinition.identifier()` after conversion. JSON source keys, file paths, and `ResourceLocation` source ids are diagnostics/source metadata only and must not become active registry keys.
- **D-06:** Full reload replacement remains a full active-registry replacement: stale entries are removed, valid replacement entries are published in deterministic iteration order, and duplicate identifier behavior must be explicit in tests if touched.
- **D-07:** `ParticlePublisher` remains the canonical publication seam unless research finds a narrower module-owned equivalent already present. Any root `ParticleAssetRegistry` facade must delegate to the particle-module publisher/store API and retain its transitional-removal documentation.

### Identifier Conversion
- **D-08:** Particle ids crossing module boundaries stay string-keyed. `ResourceLocation` adaptation belongs only at Forge/resource/MC integration boundaries, matching Phase 9-11 string-keyed packet and lookup decisions.
- **D-09:** Source `ResourceLocation` keys from reload scanning may be used for logging, error reporting, and source iteration only. They must not leak into `ParticleStore`, lookup names, spawn request ids, packet ids, or publication keys.
- **D-10:** Conversion failures should fail loudly at the conversion seam (`DataResult` or equivalent) but preserve reload-listener behavior by logging/skipping invalid resources where the existing loader already logs parse failures. Do not silently drop parity-critical fields.

### Root Compatibility
- **D-11:** Existing root entrypoints such as `BrParticleLoader`, `ParticleAssetRegistry`, `ParticleManager`, `ParticleLookup`, and `ParticleSpawnService` are compatibility adapters only. Phase 12 may update their internals, but they must not regain canonical particle loading or publication business ownership.
- **D-12:** Root compatibility should remain behavior-compatible for current callers, including animation particle effects and packet-driven spawn paths. If the active store type changes from legacy root `BrParticle` to module `ParticleDefinition`, adapters must bridge existing root callers explicitly rather than forcing unrelated Phase 13 command/network work into Phase 12.
- **D-13:** Do not introduce a broad root compatibility layer. Keep only named, deletable adapters with documented removal conditions and direct delegation into `:eyelib-particle` APIs/services.

### Verification Expectations
- **D-14:** Verification planning must use JetBrains MCP Gradle tasks only; never run Gradle through shell.
- **D-15:** Automated checks should cover reload replacement semantics, description-identifier publication, stale-entry removal, deterministic replacement order, source-key-not-used behavior, adapter delegation, and boundary scans that keep pure particle module packages root/MC/Forge-clean.
- **D-16:** Phase 12 should compile/test the particle module and targeted root tests that exercise loader/registry/publication compatibility. Broad root `:test` cleanup and visual/client smoke evidence remain Phase 14 unless a Phase 12 change directly requires a narrower targeted check.
- **D-17:** Existing assertions around `ParticlePublisher`, `ParticleAssetRegistry`, and `ParticleManager` must not be weakened or deleted to make the rewire compile; update them to the new canonical store/type only when they still prove the same observable behavior.

### Claude's Discretion
- No user-only gray area remains after applying Phase 9-11 decisions and Phase 12 requirements. Planner/executor may choose the smallest staged implementation that satisfies the ownership move, preserves root compatibility, and avoids design degradation.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project And Phase Scope
- `.planning/PROJECT.md` - v1.2 milestone goal, active loading/publication compatibility requirement, particle split constraints, and JetBrains MCP-only Gradle rule.
- `.planning/REQUIREMENTS.md` - PLOAD-01, PLOAD-02, and PLOAD-03 requirements for Phase 12; PNET and PVERIFY boundaries for later phases.
- `.planning/ROADMAP.md` - Phase 12 goal, dependency on Phase 11, success criteria, and adjacent Phase 13-14 deferrals.
- `.planning/STATE.md` - accumulated Phase 8-11 decisions and current post-Phase-11 continuity state.

### Prior Phase Decisions
- `.planning/phases/09-particle-api-store-seam/09-CONTEXT.md` - string-keyed API/store seam, `ParticlePublisher`, description-identifier publication rule, and root transitional facade policy.
- `.planning/phases/10-schema-runtime-ownership-adapter/10-CONTEXT.md` - importer raw schema owner, particle runtime definition owner, `ParticleDefinitionAdapter`, and root legacy `BrParticle` non-canonical status.
- `.planning/phases/11-runtime-client-core-extraction/11-CONTEXT.md` - runtime extraction boundary, root adapter compatibility, side-safe client integration, and explicit deferral of loading/publication to Phase 12.
- `.planning/phases/11-runtime-client-core-extraction/11-VERIFICATION.md` - verified Phase 11 behavior, module-owned runtime/client integration, current conversion path, and deferred broad/root test cleanup.
- `.planning/phases/11-runtime-client-core-extraction/11-REVIEW.md` - clean review status after Phase 11 fixes and targeted JetBrains MCP verification evidence.

### Repository Boundary Rules
- `AGENTS.md` - repository editing, reading, Gradle, module update, and verification rules.
- `MODULES.md` - particle subproject responsibility, client particle compatibility adapter row, loader/manager/registry responsibilities, and module update rules.
- `docs/index/repo-map.md` - repository navigation and particle-module/current-root-adapter starting points.
- `docs/architecture/01-module-boundaries.md` - target ownership map, particle module boundary, loader/registry publication patterns, and root legacy `BrParticle` status.
- `docs/architecture/02-side-boundaries.md` - particle side rules, pure runtime cleanliness, client integration constraints, and ResourceLocation/string boundary guidance.

### Particle Package Documentation
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` - particle module scope, dependency direction, Phase 12 responsibility, current consumers, and verification rule.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` - retained root particle adapter boundaries, Phase 12 deferral note, and string-keyed packet/runtime adaptation rule.
- `src/main/java/io/github/tt432/eyelib/client/loader/README.md` - loader pattern, current reload orchestration, root-side runtime adaptation, and publication-through-registry rule.
- `src/main/java/io/github/tt432/eyelib/client/registry/README.md` - runtime publication boundary, `ParticleAssetRegistry` transitional role, and `ParticlePublisher` ownership rule.

### Codebase Scout Maps
- `.planning/codebase/ARCHITECTURE.md` - manager, loader, registry, lookup, resource reload, and sync-lane patterns.
- `.planning/codebase/INTEGRATIONS.md` - Forge/Minecraft integration, development run configuration context, and subproject dependency cautions. Note: this map predates the active `:eyelib-particle` inclusion and must be cross-checked against current docs.
- `.planning/codebase/TESTING.md` - JUnit 5 conventions, fixture patterns, boundary/static test style, and JetBrains MCP-only examples.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/io/github/tt432/eyelib/client/loader/BrParticleLoader.java` currently scans `eyelib/particles/*.json`, parses root legacy `BrParticle.CODEC`, replaces an internal source-keyed map, and calls `ParticleAssetRegistry.replaceParticles(...)`.
- `src/main/java/io/github/tt432/eyelib/client/loader/SimpleJsonWithSuffixResourceReloadListener.java` owns suffix/resource scanning behavior through `FileToIdConverter`; this is the observable reload path Phase 12 must preserve.
- `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/ClientLoaderLifecycleHooks.java` owns side-gated Forge reload-listener registration and currently registers `BrParticleLoader.INSTANCE` with the other client loaders.
- `src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java` already delegates to `io.github.tt432.eyelibparticle.api.ParticlePublisher` and extracts identifiers from `particle.particleEffect().description().identifier()`.
- `src/main/java/io/github/tt432/eyelib/client/manager/ParticleManager.java` is currently the root backing adapter for `ParticleStore<BrParticle>` and preserves replacement/clear behavior through manager storage.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/ParticleStore.java` and `ParticlePublisher.java` provide the string-keyed store/publication contracts and preserve replacement order through `LinkedHashMap`.
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java` and `ParticleDefinitionAdapter.java` provide the canonical runtime definition and importer-schema conversion seam that Phase 12 should use instead of root legacy schema ownership.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` currently converts legacy root `BrParticle` to importer schema and then `ParticleDefinition` at spawn time; Phase 12 can remove or narrow this late conversion only if compatibility callers remain covered.

### Established Patterns
- Resource reload parsing is split from runtime publication: loaders parse into local maps, then domain registry seams publish to manager-backed stores.
- Publication should go through domain-specific registry/publisher seams, not direct manager writes in loaders.
- Root runtime may consume `:eyelib-particle`; `:eyelib-particle` must not import root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes.
- Pure particle runtime/API packages must remain root/MC/Forge-clean; Minecraft/Forge-facing reload or client hooks require explicit side-safe integration ownership.
- Tests use flat JUnit 5 classes, real codecs/fixtures where possible, hand-written test doubles, and source scans for boundary invariants.

### Integration Points
- Forge reload lifecycle connects through `mc/impl/client/loader/ClientLoaderLifecycleHooks.java`; this can keep registering a root adapter or register a side-safe particle-module listener if introduced.
- Runtime publication connects through `ParticleAssetRegistry.publisher()` / `ParticlePublisher` / `ParticleStore`; this is the seam to move from root ownership to particle-module ownership.
- Existing root callers still use `ParticleLookup.get(...)` and `ParticleSpawnService.spawnEmitter(...)` from animation/runtime paths; Phase 12 must either preserve these adapters or migrate the callers with equivalent behavior.
- Current tests already assert description-identifier publication and source-key rejection in `ParticleAssetRegistryTest`, `ParticleAssetRegistryPublisherAdapterTest`, `ParticlePublisherTest`, and `ParticleManagerStoreAdapterTest`.

</code_context>

<specifics>
## Specific Ideas

Phase 12 should be a behavior-preserving ownership rewire, not a user-visible feature change. The preferred default is to make module-owned particle loading/publication consume importer schema and publish module runtime definitions by description identifier, while keeping named root adapters only where current root callers still need them.

</specifics>

<deferred>
## Deferred Ideas

- `/eyelib particle` command syntax, suggestions, validation, spawn position behavior, success message, and packet integration rewires remain Phase 13 scope.
- Existing particle-related test relocation/adaptation, broad root test-suite cleanup, ClientSmoke flow decisions, hardware/manual rendering evidence, and final documentation gate remain Phase 14 scope.
- Any packaging decision to publish `:eyelib-particle` as an independent external artifact remains future requirement PFUT-03, not Phase 12 scope.

</deferred>

---

*Phase: 12-Loading & Publication Rewire*
*Context gathered: 2026-05-09*
