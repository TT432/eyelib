# Client Particle Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/particle/`
- Client particle runtime, emitters, render manager, and lookup/spawn boundaries.

## Phase 10 Ownership Status
- `io.github.tt432.eyelibimporter.particle.BrParticle` is the canonical raw Bedrock particle schema/codec owner.
- `io.github.tt432.eyelibparticle.runtime.ParticleDefinition` is the canonical module runtime definition owner, produced through `ParticleDefinitionAdapter` using the allowed particle -> importer dependency for ParticleDefinitionAdapter.
- `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java` is a legacy/non-canonical runtime adapter target kept for compatibility until later extraction phases.
- The adapter-owned mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
- Phase 11 moved executable runtime core in stages: emitter and particle component execution, emitter/particle lifecycle, render-manager behavior, render adapter behavior, and `Dist.CLIENT` client hook ownership now live in `:eyelib-particle`.
- Phase 12 loading/publication ownership now lives in `:eyelib-particle`: `ParticleDefinitionRegistry` owns the active `ParticleStore<ParticleDefinition>`, `ParticleResourcePublication` parses importer `BrParticle` JSON and converts through `ParticleDefinitionAdapter`, and active publication is keyed by `ParticleDefinition.identifier()`.
- Phase 13 rewires command/network integration, and Phase 14 owns final broad/client verification evidence.

## Current Runtime Boundaries
- `ParticleLookup.java`: transitional read-side root facade delegating active names and definitions to `io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry`; legacy root `BrParticle` reads remain compatibility-only. Removal condition: delete after root callers migrate directly to particle API/loading adapters/services.
- `ParticleSpawnService.java`: transitional root runtime adapter delegating packet spawn/remove entrypoints through `io.github.tt432.eyelibparticle.api.ParticleSpawnApi`, module-owned `io.github.tt432.eyelibparticle.client.ParticleRenderManager`, and active definitions from `ParticleDefinitionRegistry`; removal condition: delete after packet/runtime callers migrate directly to particle API/client runtime services.
- `ParticleManager.java`: transitional root compatibility map for legacy `BrParticle` callers only; it must not own active loading/publication business logic.
- `bedrock/BrParticleRenderManager.java`: transitional root compatibility adapter over module-owned `ParticleRenderManager`; removal condition: delete after instrumentation and remaining root callers bind directly to particle-module client services.
- Spawn request state is owned by `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest`; do not add a duplicate root request type.

## Communication Rule
- Runtime reads should use `ParticleLookup.java`; packet application should use `ParticleSpawnService.java`.
- Packet/runtime adaptation may decode Minecraft identifiers at the service boundary, but request/state seams should stay platform-type-free.
- Spawn/remove packet application must preserve string-keyed request intent and route executable runtime/render work through the particle module runtime/client services without changing packet payloads.
- Phase 13 command/network integration keeps `NetClientHandlers` context-free: `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)` reach this package only through `ParticleSpawnService`, which converts packet fields into module `ParticleSpawnRequest` and delegates runtime work into module APIs/client services.
- `ResourceLocation` adaptation for particle reload belongs in root Forge/resource integration (`BrParticleLoader`) only. Active lookup/spawn names remain strings from `ParticleDefinition.identifier()`.

## Boundary Reminder
- Networking code should call particle lookup/spawn services here instead of reading loader internals directly.
- Retained root facades are transitional compatibility adapters only; new particle API/store consumers should prefer `io.github.tt432.eyelibparticle.api` contracts where practical.
- Root facades must not reintroduce root-owned particle loading/publication internals. Canonical loading publication lives in `io.github.tt432.eyelibparticle.loading`.
- Final ownership remains split: `:eyelib-particle` owns module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, executable runtime, client integration, render manager, and loading/publication through `ParticleDefinitionRegistry` plus `ParticleResourcePublication`; root owns `BrParticleLoader`, compatibility adapters, `mc/impl/common/command`, `mc/impl/network/packet`, transport, and `NetClientHandlers` delegation; importer owns raw `io.github.tt432.eyelibimporter.particle.BrParticle` schema/codec.
- PFUT-02 packet-contract relocation, PFUT-03 independent particle artifact publication, unrelated fixture cleanup, and broad ClientSmoke/hardware/manual visual proof remain deferred or non-blocking outside Phase 13; JetBrains MCP Gradle tasks provide the automated verification path, and normal source tests must not read `.planning/` files.
