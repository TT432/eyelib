# Client Particle Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Phase 10 Ownership Status
- `io.github.tt432.eyelibimporter.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner.
- `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` is the canonical module runtime definition owner, produced through `ParticleDefinitionAdapter` using the allowed particle -> importer dependency for ParticleDefinitionAdapter.
- Root `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` has been deleted; importer `io.github.tt432.eyelibimporter.particle.BrParticle` remains the canonical raw schema/codec owner.
- The adapter-owned mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Phase 11 moved executable runtime core in stages: emitter and particle component execution, emitter/particle lifecycle, render-manager behavior, render adapter behavior, and `Dist.CLIENT` client hook ownership now live in `:eyelib-particle`.
- Phase 12 loading/publication ownership now lives in `:eyelib-particle`: `ParticleDefinitionRegistry` owns the active `ParticleStore<ParticleDefinition>`, `ParticleResourcePublication` parses importer `BrParticle` JSON and converts through `ParticleDefinitionAdapter`, and active publication is keyed by `ParticleDefinition.identifier()`.
- Phase 13 rewires command/network integration, and Phase 14 owns final broad/client verification evidence.

## Current Runtime Boundaries
- Legacy root `bedrock/**`, `ParticleLookup`, `ParticleManager`, and `ParticleAssetRegistry` have been deleted after production callers moved to module-owned definitions/publication and tests moved off the root compatibility schema.
- `ParticleSpawnService.java`: transitional root facade converting packet fields into `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest` and supplying root Minecraft/capability context to module-owned `io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter`; it no longer accepts root `BrParticle` or `BrParticleEmitter` overloads. Removal condition: delete after packet/runtime callers migrate directly to particle API/client runtime services.
- Root `bedrock/BrParticleRenderManager.java` has also been deleted.
- Executable runtime remains in `io.github.tt432.eyelibparticle.runtime.bedrock/**`, including component equivalents under `io.github.tt432.eyelibparticle.runtime.bedrock.component/**`.
- Spawn request state is owned by `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest`; do not add a duplicate root request type.

## Communication Rule
- New active definition/name reads should use `io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry`; packet application should use `ParticleSpawnService.java` until packet callers bind directly to module client runtime services.
- Packet/runtime adaptation may decode Minecraft identifiers at the service boundary, but request/state seams should stay platform-type-free.
- Spawn/remove packet application must preserve string-keyed request intent and route executable runtime/render work through the particle module runtime/client services without changing packet payloads.
- Particle command/network integration keeps `NetClientHandlers` context-free: particle-owned `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)` reach this package only through `ParticleSpawnService`, which converts packet fields into module `ParticleSpawnRequest` and delegates particle-only spawn/runtime work into `ParticleSpawnRuntimeAdapter`.
- `ResourceLocation` adaptation for particle reload belongs in root Forge/resource integration (`BrParticleLoader`) only. Active lookup/spawn names remain strings from `ParticleDefinition.identifier()`.

## Boundary Reminder
- Networking code should call particle spawn services here instead of reading loader internals directly.
- Do not reintroduce root-owned particle loading/publication internals. Canonical loading publication lives in `io.github.tt432.eyelibparticle.loading`.
- Final ownership remains split: `:eyelib-particle` owns module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, executable runtime, client integration, render manager, particle packet contracts under `io.github.tt432.eyelibparticle.network`, and loading/publication through `ParticleDefinitionRegistry` plus `ParticleResourcePublication`; root owns `BrParticleLoader`, the temporary `ParticleSpawnService` context adapter, `mc/impl/common/command`, transport registration, and `NetClientHandlers` delegation; importer owns raw `io.github.tt432.eyelibimporter.particle.BrParticle` schema/codec.
- PFUT-03 independent particle artifact publication, unrelated fixture cleanup, and broad ClientSmoke/hardware/manual visual proof remain deferred or non-blocking outside this packet relocation; JetBrains MCP Gradle tasks provide the automated verification path, and normal source tests must not read `.planning/` files.
