# MC Command Implementation

## Scope
- Path: `mc/impl/common/command/`
- Brigadier command registration, `ResourceLocation` adaptation, and Forge event wiring.

## Key Responsibilities
- `EyelibParticleCommand` owns Forge `RegisterCommandsEvent` subscription, Brigadier tree wiring, `ResourceLocation` parsing/validation, `ServerPlayer` access, and particle packet dispatch.
- Uses particle packet contracts: `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and `RemoveParticlePacket(String removeId)` from `io.github.tt432.eyelibparticle.network`.

## Dependency Direction
- Command implementation may depend on `mc/impl/common/command` for Brigadier/Forge wiring.
- Particle command shaping delegates to platform-free `ParticleCommandRuntime`.
