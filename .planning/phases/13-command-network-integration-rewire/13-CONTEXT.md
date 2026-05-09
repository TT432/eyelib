# Phase 13: Command & Network Integration Rewire - Context

**Gathered:** 2026-05-09
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 13 rewires the user-facing `/eyelib particle` command and particle spawn/remove network path so they remain behavior-compatible after the particle module extraction. The phase must preserve command syntax, suggestions, validation, spawn position behavior, success message, and string-keyed spawn/remove packet behavior while keeping Minecraft/Forge command, player, packet channel, and identifier concerns in explicit root/MC integration adapters. Pure `:eyelib-particle` APIs must stay root-independent and platform-light.

</domain>

<decisions>
## Implementation Decisions

### Command Compatibility
- **D-01:** `/eyelib particle` remains the user-facing command shape. Preserve the existing `eyelib particle <effect> [position]` syntax, the optional position argument, current source-position fallback, server-player-only behavior, and success result semantics unless research finds a direct regression bug in the existing behavior.
- **D-02:** The visible success message remains compatible with `ParticleCommandRuntime.spawnSuccessMessage(...)`: `已生成粒子: {particleId} @ {x}, {y}, {z}`. Do not silently change message language, coordinate formatting, or success text in this phase.
- **D-03:** Command execution may continue to generate a fresh string spawn id per invocation. The spawn id is transport/runtime state, not a particle definition identifier and not a `ResourceLocation` object.
- **D-04:** Command changes should be adapter-level rewires only. Do not add new command capabilities, aliases, permission policy, batch spawning, or removal command behavior in Phase 13.

### Suggestion And Validation Ownership
- **D-05:** Suggestion filtering and deterministic request/message shaping belong in platform-free command runtime helpers such as `src/main/java/io/github/tt432/eyelib/common/runtime/ParticleCommandRuntime.java`.
- **D-06:** Minecraft/Brigadier-specific validation and parsing stay in `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java` or an equivalently explicit MC integration adapter. `ResourceLocationArgument`, `CommandSourceStack`, `ServerPlayer`, `Vec3Argument`, and `Component` must not leak into pure particle module APIs.
- **D-07:** Suggestions should continue to be sourced from active particle definitions through the root transitional lookup facade or a narrow particle-module lookup adapter, but the values exposed to Brigadier remain strings keyed by `ParticleDefinition.identifier()`.
- **D-08:** Invalid active ids should be filtered before Brigadier suggests them, but definition lookup/spawn should still tolerate missing ids by no-oping at the runtime adapter rather than crashing the client path.

### Packet Contract Ownership
- **D-09:** Spawn/remove packet payloads remain string-keyed: `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)`. Do not convert these packet contracts back to `ResourceLocation` or legacy root `BrParticle` payloads.
- **D-10:** Packet DTO/codec ownership currently lives under `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/`; keep packet encoding/decoding and `FriendlyByteBuf` usage in the MC/network integration layer, not in pure `:eyelib-particle` packages.
- **D-11:** Packet registration and transport stay in `EyelibNetworkTransport`/network integration ownership. Phase 13 may adjust registration or handler wiring only to improve delegation boundaries while preserving message ids/order compatibility expectations as much as the current Forge registration model allows.
- **D-12:** Future relocation of packet contracts into a dedicated cross-module transport contract remains deferred by PFUT-02. Phase 13 should not introduce a broad new transport module.

### Network Handler Delegation
- **D-13:** `NetClientHandlers` remains a context-free delegation layer. It should route spawn/remove packets into a particle service boundary instead of touching render manager internals, loader internals, or particle definition stores directly.
- **D-14:** Runtime spawn/remove work should continue through `ParticleSpawnService` or a narrower explicit adapter that delegates into `io.github.tt432.eyelibparticle.api.ParticleSpawnApi`, `ParticleDefinitionRegistry`, and module-owned `ParticleRenderManager` without exposing render internals in network handlers.
- **D-15:** Keep `ParticleSpawnRequest` as the module-owned request seam. Do not add a duplicate root spawn request type or make network handlers construct render/runtime classes directly.
- **D-16:** Missing particle definitions, missing `Minecraft.getInstance().player`, or missing client level remain safe no-op conditions at the root runtime adapter boundary. Do not turn network packet handling into a user-visible crash path.

### Identifier Conversion
- **D-17:** Particle ids crossing command, lookup, request, and packet seams stay as strings. `ResourceLocation` exists only at Minecraft/Brigadier/resource integration boundaries for validation/parsing and must be converted to `String` before entering particle request, lookup, publication, or packet payload seams.
- **D-18:** Active particle ids are `ParticleDefinition.identifier()` values published by Phase 12 loading/publication. JSON resource paths, source `ResourceLocation` keys, and reload source ids remain diagnostics/report metadata only.
- **D-19:** Command suggestions and packet spawn lookup should resolve against active module definitions, not the legacy root `ParticleManager` map or root `client/particle/bedrock/BrParticle` as canonical state.

### Verification Expectations
- **D-20:** Verification planning must use JetBrains MCP Gradle tasks only; never run Gradle through shell.
- **D-21:** Automated Phase 13 checks should cover command runtime suggestion filtering, request construction, success message compatibility, command adapter wiring, string-keyed spawn/remove packet shapes, packet codec round-trip if available, network handler delegation, and source-scan boundaries preventing pure particle packages from importing root/MC/Forge command or network classes.
- **D-22:** Existing Phase 11/12 particle runtime/loading tests must not be weakened to make command/network rewiring compile. Update or add targeted tests so the same observable behavior remains proven.
- **D-23:** Broad root `:test` cleanup, final ClientSmoke flow decisions, and hardware/manual visual rendering evidence remain Phase 14 scope unless Phase 13 introduces a direct targeted regression that must be verified immediately.

### Claude's Discretion
- No user-only gray area remains after applying the roadmap, requirements, Phase 9-12 decisions, and current code evidence. Planner/executor may choose the smallest staged implementation that preserves compatibility and clarifies adapter ownership without adding new user-visible behavior.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project And Phase Scope
- `.planning/PROJECT.md` - v1.2 milestone goal, command/network compatibility requirement, particle split constraints, and JetBrains MCP-only Gradle rule.
- `.planning/REQUIREMENTS.md` - PNET-01, PNET-02, PNET-03 requirements and Phase 14 verification deferrals.
- `.planning/ROADMAP.md` - Phase 13 goal, dependency on Phase 12, success criteria, and Phase 14 boundary.
- `.planning/STATE.md` - accumulated Phase 8-12 decisions and current Phase 13 planning position.

### Prior Phase Evidence
- `.planning/phases/11-runtime-client-core-extraction/11-VERIFICATION.md` - verified string-keyed packet entrypoints, root `ParticleSpawnService` delegation, module runtime/client integration, and Phase 14 deferrals.
- `.planning/phases/12-loading-publication-rewire/12-CONTEXT.md` - loading/publication ownership decisions, string-keyed id policy, root compatibility adapter policy, and explicit deferral of command/network to Phase 13.
- `.planning/phases/12-loading-publication-rewire/12-VERIFICATION.md` - verified active registry ownership, packet-driven spawn lookup through module definitions, animation/controller particle effect lookup, and targeted JetBrains MCP verification evidence.
- `.planning/phases/12-loading-publication-rewire/12-REVIEW.md` - clean Phase 12 review status and changed-file inventory relevant to Phase 13 handoff.

### Repository Boundary Rules
- `AGENTS.md` - repository reading, editing, Gradle, module update, and verification rules.
- `MODULES.md` - particle subproject responsibility, command module, network/sync module, root client particle compatibility adapter row, and module update rules.
- `docs/index/repo-map.md` - repository navigation and particle/network starting points.
- `docs/index/network.md` - network/sync package reading order and packet-routing hotspots.
- `docs/architecture/01-module-boundaries.md` - target ownership map, command integration zone, particle module boundary, packet/string boundary notes, and root adapter policy.
- `docs/architecture/02-side-boundaries.md` - sync side rules, packet handler delegation rule, particle API cleanliness, and ResourceLocation/string conversion guidance.

### Package Documentation
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` - particle module scope, dependency direction, command/network Phase 13 boundary, pure-core cleanliness, and verification rule.
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md` - root particle adapter boundaries, `ParticleSpawnRequest`, packet/runtime adaptation, and string-keyed lookup/spawn rules.
- `src/main/java/io/github/tt432/eyelib/network/README.md` - network package scope, packet transport/DTO ownership under `mc/impl/network`, and transport-agnostic handler guidance.
- `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/README.md` - requested package README, but currently missing. Downstream agents should inspect the package source and consider whether Phase 13 documentation should add this README if command ownership changes.

### Codebase Scout Maps
- `.planning/codebase/ARCHITECTURE.md` - sync lane, particle spawn service role, manager/loader/lookup patterns, and `mc/impl` quarantine model. Note: this map predates current `:eyelib-particle` status and must be cross-checked against current docs.
- `.planning/codebase/INTEGRATIONS.md` - Forge runtime, `SimpleChannel`, development environment, and subproject dependency cautions. Note: this map predates current `:eyelib-particle` inclusion and must be cross-checked against current docs.
- `.planning/codebase/STACK.md` - Java 17, Forge 1.20.1, Gradle/JetBrains MCP environment, and testing stack.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java` owns Brigadier registration, `ResourceLocationArgument` parsing, active-id suggestions, player resolution, position fallback, packet send, and success message emission.
- `src/main/java/io/github/tt432/eyelib/common/runtime/ParticleCommandRuntime.java` already provides platform-free suggestion filtering, spawn request shaping, and success message formatting.
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/SpawnParticlePacket.java` and `RemoveParticlePacket.java` already use string-keyed packet payload records with `FriendlyByteBuf` codecs in the MC/network packet layer.
- `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java` already delegates particle packets to `ParticleSpawnService` without touching render manager internals directly.
- `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` already bridges packets into module-owned `ParticleSpawnRequest`, active `ParticleDefinitionRegistry` lookup, `BedrockParticleRuntime`, and module `ParticleRenderManager`.
- `src/main/java/io/github/tt432/eyelib/mc/impl/network/EyelibNetworkTransport.java` owns `SimpleChannel`, message registration, side-aware dispatch, and player send helpers.
- `src/test/java/io/github/tt432/eyelib/common/runtime/ParticleCommandRuntimeTest.java`, `src/test/java/io/github/tt432/eyelib/network/SpawnParticlePacketTest.java`, and `src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java` provide existing targeted coverage to preserve or extend.

### Established Patterns
- Platform-specific Forge/Minecraft wiring belongs under `mc/impl/**`; platform-free shaping logic can live in root common runtime helpers or module APIs if it stays root/MC/Forge-clean.
- Packet handlers should remain thin routing methods into domain services; they should not own rendering, loading, or store lookup internals.
- Particle active lookup now flows through module-owned `ParticleDefinitionRegistry` and string identifiers from `ParticleDefinition.identifier()`.
- Retained root particle classes are transitional compatibility adapters only; they may bridge current callers but must not regain canonical particle loading, publication, or render-manager ownership.
- Tests commonly combine behavioral JUnit assertions with source-scan boundary tests to lock import and delegation invariants.

### Integration Points
- Command registration connects through Forge `RegisterCommandsEvent` in `EyelibParticleCommand`.
- Command execution sends `SpawnParticlePacket` through `EyelibNetworkTransport.sendToPlayer(...)` to the invoking `ServerPlayer`.
- Network registration connects `SpawnParticlePacket` and `RemoveParticlePacket` to `NetClientHandlers` through `EyelibNetworkTransport.register()`.
- Client packet application connects to `ParticleSpawnService.spawnFromPacket(...)` and `ParticleSpawnService.removeEmitter(...)`, which then cross into module runtime/client services.
- Active particle names for suggestions connect through `ParticleLookup.names()`, currently backed by `ParticleDefinitionRegistry.store().names()` after Phase 12.

</code_context>

<specifics>
## Specific Ideas

Phase 13 should be a compatibility-preserving adapter rewire, not a feature expansion. The pragmatic default is to keep command and transport types in root/MC integration packages, keep packet/request ids as strings, delegate all executable particle work into particle services/module runtime, and add targeted tests around the exact command/network behavior users can observe.

</specifics>

<deferred>
## Deferred Ideas

- Existing particle-related test relocation/adaptation beyond Phase 13 targeted coverage, broad root test-suite cleanup, final ClientSmoke flow, hardware/manual visual rendering evidence, and final architecture documentation gate remain Phase 14 scope.
- Permanent packet-contract relocation into a dedicated cross-module transport contract remains future requirement PFUT-02, not Phase 13 scope.
- Publishing `:eyelib-particle` as an independent external artifact remains PFUT-03/future packaging scope.

</deferred>

---

*Phase: 13-Command & Network Integration Rewire*
*Context gathered: 2026-05-09*
