# Phase 13 — Command & Network Integration Rewire Research

**Phase:** 13 — Command & Network Integration Rewire  
**Date:** 2026-05-09  
**Status:** Complete  

## Research Question

What must be known to plan a compatibility-preserving rewire of `/eyelib particle` and particle spawn/remove networking after the particle module extraction?

## Findings

### Current Command Path

- `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java` is already the correct MC/Brigadier adapter location.
- It registers `eyelib particle <effect> [position]`, parses `ResourceLocationArgument.id()`, uses `Vec3Argument.vec3()` for the optional position, falls back to `CommandSourceStack.getPosition()`, catches non-player sources by returning `0`, sends `SpawnParticlePacket`, and emits `ParticleCommandRuntime.spawnSuccessMessage(...)`.
- `src/main/java/io/github/tt432/eyelib/common/runtime/ParticleCommandRuntime.java` already owns platform-free suggestion filtering, spawn request construction, and success message formatting.
- Existing `ParticleCommandRuntimeTest` covers case-insensitive filtering, invalid-id rejection through an injected predicate, request construction, and the Chinese success message.

### Current Network Path

- `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)` already live under `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/` and encode strings through `EyelibStreamCodecs.STRING`.
- `EyelibNetworkTransport.register()` registers `RemoveParticlePacket` before `SpawnParticlePacket` and dispatches both through `NetClientHandlers` on the client side.
- `NetClientHandlers` is a thin context-free route table: spawn delegates to `ParticleSpawnService.spawnFromPacket(packet)`, remove delegates to `ParticleSpawnService.removeEmitter(packet.removeId())`.
- `ParticleSpawnService.spawnFromPacket` converts packet fields into module-owned `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest` and delegates through `ParticleSpawnApi`.
- Missing active definitions, missing client player, or missing client level are already no-op conditions in `RootParticleSpawnApi.spawn`.

### Boundary Implications

- `ResourceLocation` must remain at MC/Brigadier and transport-channel boundaries only. The command adapter can parse a `ResourceLocation`, but it must pass `id.toString()` to runtime request shaping and packet payloads.
- `:eyelib-particle` pure packages must not import root runtime, root network, root capability, root `mc/impl`, Minecraft, or Forge classes. Existing boundary tests already scan this invariant for non-client particle module sources.
- Packet DTO/codec ownership should stay in `mc/impl/network/packet`; do not introduce a new transport module in Phase 13.
- Root network package docs now say packet contracts moved under `mc/impl/network/packet`, so source scans should target both `network/` routing and `mc/impl/network/packet/` packet shape.

## Recommended Implementation Strategy

1. Add/strengthen tests before changing behavior:
   - Command runtime compatibility and command adapter source-boundary tests.
   - Packet shape, codec/decode text contract, handler delegation, transport registration order, and no direct render-manager/loader/store access in `NetClientHandlers`.
2. Make only the smallest adapter rewires needed to satisfy tests:
   - Keep command shape and message unchanged.
   - Keep string ids at runtime/request/packet seams.
   - Keep packet handlers thin and context-free.
3. Update ownership documentation after code/test rewires land.
4. Verify through JetBrains MCP Gradle tasks only.

## Validation Architecture

Use targeted JUnit/static source tests plus JetBrains MCP Gradle execution.

Required automated checks:

- Root targeted command/network tests via JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames=[":test"]` and `scriptParameters` containing targeted `--tests` filters for:
  - `io.github.tt432.eyelib.common.runtime.ParticleCommandRuntimeTest`
  - `io.github.tt432.eyelib.mc.impl.common.command.EyelibParticleCommandBoundaryTest`
  - `io.github.tt432.eyelib.network.SpawnParticlePacketTest`
  - `io.github.tt432.eyelib.network.RemoveParticlePacketTest`
  - `io.github.tt432.eyelib.network.ParticleNetworkDelegationBoundaryTest`
  - `io.github.tt432.eyelib.docs.ParticleCommandNetworkDocumentationTest`
- Compile gate via JetBrains MCP `jetbrain_run_gradle_tasks` with `taskNames=[":compileJava", ":eyelib-particle:compileJava"]`.
- No shell Gradle commands.

## Common Pitfalls

- Converting packet payloads back to `ResourceLocation` would violate PNET-02 and D-09/D-17.
- Moving `FriendlyByteBuf` packet codecs into `:eyelib-particle` would contaminate pure module boundaries and violate D-10.
- Letting `NetClientHandlers` reach into `ParticleRenderManager`, `ParticleDefinitionRegistry`, or loader internals directly would violate D-13/D-14.
- Changing success message language/format would break D-02.
- Adding aliases, removal command behavior, permissions, or batch behavior would violate D-04.

## Source Coverage Audit

| Source | Item | Coverage Plan |
|--------|------|---------------|
| GOAL | Command and network behavior remain compatible while platform concerns stay in explicit adapters | Plans 13-01, 13-02, 13-03 |
| REQ | PNET-01 | Plan 13-01 |
| REQ | PNET-02 | Plan 13-02 |
| REQ | PNET-03 | Plans 13-01, 13-02, 13-03 |
| CONTEXT | D-01 through D-08 command/suggestion/validation decisions | Plan 13-01 |
| CONTEXT | D-09 through D-12 packet ownership decisions | Plan 13-02 |
| CONTEXT | D-13 through D-16 network handler delegation decisions | Plan 13-02 |
| CONTEXT | D-17 through D-19 identifier conversion/active lookup decisions | Plans 13-01, 13-02 |
| CONTEXT | D-20 through D-23 verification expectations | Plan 13-03 |
| RESEARCH | Keep packet contracts in `mc/impl/network/packet`, command adapter in `mc/impl/common/command`, and pure shaping in `ParticleCommandRuntime` | Plans 13-01, 13-02, 13-03 |

## Research Complete

Phase 13 is ready for executable planning.
