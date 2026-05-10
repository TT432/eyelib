# MC Impl Command Index

## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/`
- Owns Phase 13 command/network integration for the `/eyelib particle` Minecraft/Forge command adapter.

## Key Files
- `EyelibParticleCommand.java`: registers the Brigadier command shape, parses `ResourceLocationArgument`, reads `CommandSourceStack`, resolves `ServerPlayer`, reads optional `Vec3Argument`, emits `Component` success messages, converts parsed ids to string identifiers, and dispatches string-keyed particle packets through MC transport.
- `src/main/java/io/github/tt432/eyelib/common/runtime/ParticleCommandRuntime.java`: owns deterministic platform-free suggestion filtering, spawn request construction, and success-message shaping. It must stay free of Minecraft/Forge command types.

## Phase 13 Ownership
- `ParticleCommandRuntime` owns request/message shaping only; it receives strings and coordinates, not `ResourceLocation` or Brigadier state.
- `EyelibParticleCommand` owns platform parsing and conversion to string ids before building requests or packets.
- Packet dispatch remains string-keyed through particle-owned `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)`; remove packet compatibility is covered by `RemoveParticlePacket(String removeId)` in `io.github.tt432.eyelibparticle.network`.
- Do not add aliases, remove-command behavior, batch spawning, permission changes, loader access, or direct particle render-manager access in this command adapter.

## Verification Rule
- Verify command adapter boundaries with JetBrains MCP Gradle tasks only; never run Gradle through shell.
- Broad ClientSmoke/hardware visual evidence remains Phase 14 scope.
