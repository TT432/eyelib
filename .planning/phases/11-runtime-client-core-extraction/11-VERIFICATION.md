---
phase: 11-runtime-client-core-extraction
verified: 2026-05-09T10:19:50Z
status: passed
score: 10/10 must-haves verified
overrides_applied: 0
deferred:
  - truth: "Final visual/client smoke or hardware proof of real Minecraft particle rendering"
    addressed_in: "Phase 14"
    evidence: "Phase 14 success criteria require planned compile/test checks, automated ClientSmoke where applicable, and separate hardware/manual checks for runtime behavior that cannot be automatically asserted."
  - truth: "Full broad root test-suite cleanup, including stale particle boundary invariants and unrelated fixture failures"
    addressed_in: "Phase 14"
    evidence: "Phase 14 success criteria require existing particle-related tests to be moved or adapted without weakening assertions and broad module split regression coverage."
---

# Phase 11: Runtime Client Core Extraction Verification Report

**Phase Goal:** Existing particle runtime and client rendering behavior lives under `:eyelib-particle` without weakening side boundaries or behavior.
**Verified:** 2026-05-09T10:19:50Z
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Existing client particle emitter, render manager, material/texture resolution, Molang scope, lifetime, remove semantics, tick/render lifecycle, and logout cleanup behavior are preserved after extraction. | ✓ VERIFIED | Runtime now exists under `eyelib-particle/runtime/bedrock`: `BedrockParticleEmitter` registers emitter Molang variables/curves/randoms and delegates particles via `ParticleRuntimeSpawner`; `BedrockParticleInstance` registers particle variables, dispatches components, and guards idempotent removal; `ParticleRenderManager` owns collections, render/client tick lifecycle, render delegation, and `clear()`. Targeted tests and compile gates pass. |
| 2 | Particle-specific client hooks and Forge bindings live in explicit particle integration layers, not pure particle core/API packages. | ✓ VERIFIED | `ParticleRenderHooks` and `BedrockParticleRenderer` live under `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/`; `ParticleRenderManager` has no `@SubscribeEvent`; client boundary test asserts hook delegation and renderer ownership. |
| 3 | Pure particle core remains clean of platform bindings, while platform-specific bindings are side-safe and do not introduce dedicated-server classloading regressions. | ✓ VERIFIED | Source scan found no root/Minecraft/Forge imports in `eyelibparticle/runtime/**` except a stripped-comment documentation mention; `ParticleRenderHooks` is `@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)` and client-only imports are confined to `client/**`. |
| 4 | Maintainer can follow dependency direction from root integration code into particle runtime without finding a reverse dependency back to root. | ✓ VERIFIED | `ParticleSpawnService` imports module APIs/client/runtime and constructs `BedrockParticleRuntime`; `ParticleRenderManager` is module-owned; scans/tests reject particle-module imports from `io.github.tt432.eyelib.client`, `network`, `capability`, and `mc.impl`. |
| 5 | Runtime extraction has module-owned contracts and support helpers before behavior moves. | ✓ VERIFIED | `ParticleRuntimeDefinition`, `ParticleRuntimeContext`, `ParticleRuntimeServices`, `ParticleTimer`, `ParticleBlackboard`, and `ParticleMath` exist and are covered by `ParticleRuntimeSupportTest`. |
| 6 | Emitter and particle component execution semantics live in `:eyelib-particle`. | ✓ VERIFIED | Emitter components and particle components exist under `runtime/bedrock/component/**`; `ParticleComponentManager` dispatches from `ParticleDefinition.rawComponents()`; targeted component tests cover rate/lifetime/shape and billboard/tint/lifetime/motion behavior. |
| 7 | Emitter and particle lifecycle ownership lives in `:eyelib-particle` and is testable without Forge event loading. | ✓ VERIFIED | `BedrockParticleRuntime`, `BedrockParticleEmitter`, `BedrockParticleInstance`, `ParticleRuntimeEnvironment`, and `ParticleRuntimeSpawner` are module-owned; `ParticleRuntimeLifecycleTest` exercises Molang variables, spawn count, removal, and frame dispatch without Forge events. |
| 8 | Render manager behavior lives in an explicit particle-module client integration layer. | ✓ VERIFIED | `eyelibparticle.client.ParticleRenderManager` owns emitter/particle collections and lifecycle methods; `ParticleRenderManagerLifecycleTest` verifies duplicate/no-op handling, cleanup ordering, client tick advancement, and logout-style clear. |
| 9 | Material resolution, `.png` texture suffixing, render buffers, tick stages, and logout cleanup are preserved. | ✓ VERIFIED | `BedrockParticleRenderer` uses `RenderTypeResolver.resolve(new ResourceLocation(material))`, `withSuffix(".png")`, and Minecraft buffer source; `ParticleRenderHooks` preserves render/client tick START, `AFTER_ENTITIES`, and logout clear. |
| 10 | Root compatibility entrypoints delegate to particle-module runtime services while packet-driven spawn/remove remains string-keyed and behavior-compatible. | ✓ VERIFIED | `ParticleSpawnService.spawnFromPacket` builds `ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position())`; root `BrParticleRenderManager` delegates counts/spawn/remove to module `ParticleRenderManager`; `NetClientHandlers` still calls `ParticleSpawnService`; `ParticleRuntimeDelegationBoundaryTest` verifies string-keyed packet shapes. |

**Score:** 10/10 truths verified

### Deferred Items

Items not yet met but explicitly addressed in later milestone phases.

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | Final visual/client smoke or hardware proof of real Minecraft particle rendering. | Phase 14 | Phase 14 owns automated ClientSmoke where applicable and separate hardware/manual checks for runtime behavior that cannot be automatically asserted. |
| 2 | Full broad root test-suite cleanup, including stale particle boundary invariants and unrelated fixture failures. | Phase 14 | A broad `:test` spot-check currently fails two unrelated fixture tests plus stale `ParticleApiDelegationBoundaryTest` invariant that rejects all Minecraft/Forge imports in `:eyelib-particle`; Phase 14 explicitly owns existing particle test adaptation and broad regression coverage. |

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeDefinition.java` | Runtime view over canonical `ParticleDefinition` | ✓ VERIFIED | Wraps `ParticleDefinition`; exposes identifier/material/texture/curves/events/raw components/flipbook; no `BrParticle` declaration. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/support/ParticleTimer.java` | Module-owned timing helper | ✓ VERIFIED | Exists under pure runtime support; covered by `ParticleRuntimeSupportTest`. |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleRuntimeBoundaryTest.java` | Boundary guard | ✓ VERIFIED | Walks runtime source, strips comments/string literals, rejects root/Minecraft/Forge references, and rejects module `BrParticle` duplicate declarations. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentManager.java` | Component codec/dispatch registry | ✓ VERIFIED | Uses `ParticleDefinition.rawComponents()` and exposes emitter/particle component dispatch. |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/EmitterComponentRuntimeTest.java` | Emitter component parity checks | ✓ VERIFIED | Covers raw dispatch, rate/lifetime behavior, local-space, point/box shape, and direction behavior. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/particle/ParticleParticleComponent.java` | Particle component execution interface | ✓ VERIFIED | Defines pure `ParticleAccess` port; component packages are module-owned. |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/component/ParticleComponentRuntimeTest.java` | Particle component behavior tests | ✓ VERIFIED | Covers billboard UV/size, tinting, initial speed/spin, lifetime removal, block expiration, and motion update behavior. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleEmitter.java` | Module-owned emitter lifecycle | ✓ VERIFIED | Registers `variable.emitter_*`, curves/randoms, dispatches components, creates `BedrockParticleInstance`, delegates to `ParticleRuntimeSpawner`; no root render manager import. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/bedrock/BedrockParticleInstance.java` | Module-owned particle lifecycle | ✓ VERIFIED | Registers `variable.particle_*`, dispatches particle components, tracks state, and decrements emitter count exactly once on removal. |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/bedrock/ParticleRuntimeLifecycleTest.java` | Runtime lifecycle parity coverage | ✓ VERIFIED | Verifies Molang state, spawn count, emitted particle position, lifetime, and idempotent removal. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderManager.java` | Module-owned emitter/particle collections and lifecycle methods | ✓ VERIFIED | Owns collection mutation, render/client tick methods, render adapter hook, and clear behavior; tests cover lifecycle ordering. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/ParticleRenderHooks.java` | `Dist.CLIENT` Forge event subscription wrapper | ✓ VERIFIED | Side-gated hook delegates render tick, client tick, render stage, and logout cleanup to manager. |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/BedrockParticleRenderer.java` | Minecraft render buffer/material/texture adapter | ✓ VERIFIED | Owns `PoseStack`, `VertexConsumer`, `ResourceLocation`, material resolution, `.png` texture suffixing, camera, tint, billboard, and light output. |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | Transitional root packet/runtime compatibility facade | ✓ VERIFIED | Converts legacy root particle definition at spawn boundary, constructs `BedrockParticleRuntime`, delegates spawn/remove through module `ParticleRenderManager`, and preserves packet request shape. |
| `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java` | Thin compatibility adapter | ✓ VERIFIED | Counts/spawn/remove delegate to module `ParticleRenderManager`; retained legacy `spawnParticle(BrParticleParticle)` is empty but no active construction path for legacy root emitters was found. |
| `src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java` | Root-to-module delegation and packet-shape guard | ✓ VERIFIED | Asserts module runtime delegation, adapter shape, and unchanged string-keyed spawn/remove packet fields. |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `ParticleRuntimeDefinition` | `ParticleDefinition` | Wrapper/factory method | ✓ WIRED | Constructor and `of()` require canonical `ParticleDefinition`; no module `BrParticle` duplicate. |
| `ParticleRuntimeBoundaryTest` | `runtime/**` | `Files.walk` source scan | ✓ WIRED | Test walks `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime`. |
| `ParticleRuntimeDefinition` | `ParticleComponentManager` | raw component decoding | ✓ WIRED | `ParticleComponentManager.emitterComponents(definition)` and `particleComponents(definition)` consume `rawComponents()`. |
| `ParticleComponentRuntimeTest` | `ParticleAppearanceBillboard` | behavior checks | ✓ WIRED | Test asserts billboard size/UV behavior. |
| `BedrockParticleEmitter` | `ParticleRuntimeSpawner` | emitted particle handoff | ✓ WIRED | `emit()` creates `BedrockParticleInstance` and calls `spawner.spawnParticle(particle)`. |
| `ParticleRenderHooks` | `ParticleRenderManager` | render/client tick/logout delegates | ✓ WIRED | Hook calls `onRenderTickStart()`, `onClientTickStart()`, `renderAfterEntities(...)`, and `clear()`. |
| `BedrockParticleRenderer` | `RenderTypeResolver` | material resolution and texture suffixing | ✓ WIRED | Renderer calls `RenderTypeResolver.resolve(new ResourceLocation(material))` and `withSuffix(".png")`. |
| `ParticleSpawnService` | `io.github.tt432.eyelibparticle.client.ParticleRenderManager` | module runtime service delegation | ✓ WIRED | Spawns/removes use `ParticleRenderManager.INSTANCE`; emitted particles are registered through `ParticleRenderManager.INSTANCE::spawnParticle`. |
| `NetClientHandlers` | `ParticleSpawnService` | existing packet handler calls | ✓ WIRED | `onSpawnParticlePacket` calls `ParticleSpawnService.spawnFromPacket(packet)` and remove calls `removeEmitter(packet.removeId())`. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|---|---|---|---|---|
| `ParticleSpawnService` | `ParticleDefinition` / `BedrockParticleEmitter` | `ParticleLookup.get(request.particleId())` → legacy `BrParticle.CODEC` encode → importer `BrParticle.CODEC` parse → `ParticleDefinitionAdapter.fromSchema` → `BedrockParticleRuntime.createEmitter` | Yes | ✓ FLOWING |
| `BedrockParticleEmitter` | emitter components and curves | `ParticleDefinition.rawComponents()` and `definition.curves()` | Yes | ✓ FLOWING |
| `BedrockParticleInstance` | particle components and runtime state | Emitter definition + `ParticleComponentManager.particleComponents` + environment time/block ports | Yes | ✓ FLOWING |
| `ParticleRenderManager` | emitters/particles collections | `ParticleSpawnService` and `BedrockParticleEmitter.emit()` via `ParticleRuntimeSpawner` | Yes | ✓ FLOWING |
| `BedrockParticleRenderer` | material/texture/billboard/tint/light data | `particle.emitter().definition()` + `ParticleComponentManager.particleComponents` + Minecraft render buffers | Yes | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Phase 11 particle module compile/test/root compile gate | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]` | Exit code 0 | ✓ PASS |
| Targeted Phase 11 regression/boundary suite | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":eyelib-particle:test", ":test"] scriptParameters="--tests ParticleRuntimeSupportTest --tests ParticleRuntimeBoundaryTest --tests EmitterComponentRuntimeTest --tests ParticleComponentRuntimeTest --tests ParticleRuntimeLifecycleTest --tests ParticleRenderManagerLifecycleTest --tests ParticleClientIntegrationBoundaryTest --tests ParticleSpawnServiceBoundaryTest --tests ParticleRuntimeDelegationBoundaryTest --tests ParticleDefinitionDocumentationTest"` | Exit code 0 | ✓ PASS |
| Broad root `:test` exploratory check | JetBrains MCP `jetbrain_run_gradle_tasks taskNames=[":test"]` | Exit code 1; unrelated fixture `NoSuchFileException` failures plus stale `ParticleApiDelegationBoundaryTest` old all-module Minecraft/Forge ban | ? DEFERRED |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|---|---|---|---|---|
| PRENDER-01 | 11-01 through 11-06 | Existing client particle emitter, render manager, material/texture resolution, Molang scope, lifetime, remove semantics, tick/render lifecycle, and logout cleanup behavior are preserved. | ✓ SATISFIED | Runtime/component/lifecycle/client manager code exists under `:eyelib-particle`; renderer preserves material/texture semantics; hooks preserve tick/stage/logout behavior; targeted tests pass. |
| PRENDER-02 | 11-01 through 11-06 | Client-only hooks and platform integrations are side-safe after extraction and do not introduce dedicated-server classloading regressions. | ✓ SATISFIED | Pure runtime has no root/Minecraft/Forge imports; client integration is isolated under `eyelibparticle.client`; hooks are `Dist.CLIENT`; boundary tests pass. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---:|---|---|---|
| `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java` | 49 | Empty legacy `spawnParticle(BrParticleParticle)` compatibility method | ℹ️ Info | Not a blocker: search found no active `new BrParticleEmitter` construction path; retained root spawn entrypoints convert to module emitters. Keep visible for Phase 12/13/14 cleanup. |
| `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | 113 | `return null` when legacy particle cannot convert to module definition | ℹ️ Info | Intentional nullable failure path; caller checks null before spawning. |

### Human Verification Required

None for Phase 11 gate. Real visual Minecraft rendering/client smoke evidence is explicitly deferred to Phase 14 in the current milestone roadmap.

### Gaps Summary

No Phase 11 blocking gaps found. The runtime core, executable components, lifecycle, render manager, side-safe client hooks, root compatibility delegation, and PRENDER-01/PRENDER-02 requirements are implemented and verified by source inspection plus JetBrains MCP compile/test gates.

Deferred observations are intentionally later-phase items: final visual/client evidence and broad root test-suite cleanup belong to Phase 14, while loading/publication and command/network rewires remain Phase 12 and Phase 13 scope respectively.

---

_Verified: 2026-05-09T10:19:50Z_
_Verifier: the agent (gsd-verifier)_
