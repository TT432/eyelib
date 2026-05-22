# Network Package Index

## Scope

- Path: `src/main/java/io/github/tt432/eyelib/network/`
- Minimal shared channel entrypoint, transport delegation, and context-free handler dispatch for sync routing.
- Particle packet DTO/codecs live in `io.github.tt432.eyelibparticle.network` while this package delegates handling
  only.
- Root-independent attachment packet DTO/codecs live in `io.github.tt432.eyelibattachment.network` while this package
  delegates handling only.

## Start Reading Here

1. `docs/index/network.md`
2. `docs/architecture/02-side-boundaries.md`
3. `EyelibNetworkManager.java`

## Key Files

- `EyelibNetworkManager.java`: minimal shared channel entrypoint delegating transport to `mc/impl/network/`
- `NetClientHandlers.java`: context-free client apply delegation; particle spawn/remove calls only
  `ParticleSpawnService`
- Particle packet contract classes live under `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/network/`,
  including `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)` and
  `RemoveParticlePacket(String removeId)`
- Root-independent attachment packet contracts live under
  `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment/network/`

## Boundary Reminder

- Packet transport/context ownership lives under `../mc/impl/network/`; this package should stay transport-agnostic.
- This package owns shared channel entrypoints and handler dispatch only; feature-specific protocols live in feature
  modules where dependencies allow.
- Particle packet codec/DTO ownership lives under `io.github.tt432.eyelibparticle.network`; do not grow new particle
  `FriendlyByteBuf` packet contracts here.
- Root-independent attachment packet codec/DTO ownership lives under `io.github.tt432.eyelibattachment.network`;
  root-coupled attachment update packets remain in `mc/impl/network/packet` until their root registry/data dependencies
  move.
- Remaining root packet classes under `mc/impl/network/packet` are blocker-bound to root render/model/capability/data
  owners, not a destination for new feature protocols.
- Phase 13 requires `NetClientHandlers` to remain context-free and route particle packets through
  `ParticleSpawnService`, which converts packet fields into module `ParticleSpawnRequest` and delegates particle-only
  spawn/runtime work into `ParticleSpawnRuntimeAdapter`.
- `:eyelib-particle` owns particle module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, executable runtime,
  client integration, render manager, and loading/publication through `ParticleDefinitionRegistry` plus
  `ParticleResourcePublication`; importer owns raw `io.github.tt432.eyelibimporter.particle.BrParticle`; this root
  network package owns only handler delegation and sync routing.
- PFUT-03 independent particle artifact publication, unrelated fixture cleanup, and broad ClientSmoke/hardware/manual
  visual proof remain deferred or non-blocking; verify this boundary with JetBrains MCP Gradle tasks only, and keep
  normal source tests independent from `.planning/` files.
- Data-attachment state ownership is shared with `../util/data_attach/` and should be read together when sync work is
  involved.
