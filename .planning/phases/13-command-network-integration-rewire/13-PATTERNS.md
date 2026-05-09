# Phase 13 — Pattern Map

## Scope

Phase 13 touches command adapter wiring, command runtime helpers, packet DTO/codec contracts, client packet routing, particle spawn service delegation, and ownership documentation.

## Closest Existing Analogs

| Role | File | Pattern To Preserve |
|------|------|---------------------|
| Brigadier/MC command adapter | `src/main/java/io/github/tt432/eyelib/mc/impl/common/command/EyelibParticleCommand.java` | MC parsing and `ResourceLocation` validation live in `mc/impl`; runtime helper receives strings/floats only. |
| Platform-free command shaping | `src/main/java/io/github/tt432/eyelib/common/runtime/ParticleCommandRuntime.java` | Pure deterministic helper with injected predicate/supplier; no Minecraft/Forge imports. |
| Packet contract + codec | `src/main/java/io/github/tt432/eyelib/mc/impl/network/packet/SpawnParticlePacket.java` and `RemoveParticlePacket.java` | Records with string ids and `FriendlyByteBuf` codec ownership in MC/network packet package. |
| Context-free handler routing | `src/main/java/io/github/tt432/eyelib/network/NetClientHandlers.java` | Handler methods delegate to domain services only; no render-manager/loader/store internals. |
| Runtime adapter boundary | `src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java` | Packet fields convert to module `ParticleSpawnRequest`; missing definition/player/level no-op; executable work goes through module runtime/client services. |
| Source-scan regression tests | `src/test/java/io/github/tt432/eyelib/client/particle/ParticleRuntimeDelegationBoundaryTest.java` | JUnit reads source files and asserts exact imports/calls/forbidden strings. |

## Extracted Interfaces And Contracts

### `ParticleCommandRuntime`

```java
public static List<String> suggestEffectIds(String remaining, Iterable<String> candidateIds, Predicate<String> isValidId)
public static SpawnParticleRequest buildSpawnParticleRequest(String particleId, double x, double y, double z, Supplier<String> spawnIdSupplier)
public static String spawnSuccessMessage(SpawnParticleRequest request)
public record SpawnParticleRequest(String spawnId, String particleId, float x, float y, float z)
```

### Command Adapter Contract

```java
Commands.literal("eyelib")
        .then(Commands.literal("particle")
                .then(Commands.argument("effect", ResourceLocationArgument.id())
                        .suggests(EyelibParticleCommand::suggestEffects)
                        .executes(ctx -> execute(ctx, null))
                        .then(Commands.argument("position", Vec3Argument.vec3())
                                .executes(ctx -> execute(ctx, Vec3Argument.getVec3(ctx, "position"))))));
```

### Packet Contracts

```java
public record SpawnParticlePacket(String spawnId, String particleId, Vector3f position)
public record RemoveParticlePacket(String removeId)
```

### Handler Delegation Contract

```java
public static void onRemoveParticlePacket(RemoveParticlePacket packet) {
    ParticleSpawnService.removeEmitter(packet.removeId());
}

public static void onSpawnParticlePacket(SpawnParticlePacket packet) {
    ParticleSpawnService.spawnFromPacket(packet);
}
```

## Test Patterns To Reuse

- Use package-private JUnit Jupiter test classes and package-private `@Test` methods.
- Use source-file assertions with `Files.readString(Path.of("..."))` for boundary invariants that should not instantiate Minecraft/Forge runtime objects.
- Prefer exact string assertions for adapter seams, for example:
  - `id.toString()` before request/packet seams.
  - `new SpawnParticlePacket(request.spawnId(), request.particleId(), new Vector3f(request.x(), request.y(), request.z()))`.
  - `ParticleSpawnService.spawnFromPacket(packet)` and `ParticleSpawnService.removeEmitter(packet.removeId())`.
- Use regex only when asserting record signatures across whitespace.

## Boundary Rules

- `ResourceLocationArgument`, `CommandSourceStack`, `ServerPlayer`, `Vec3Argument`, `Component`, `SimpleChannel`, `NetworkEvent.Context`, and `FriendlyByteBuf` stay in explicit MC/Forge integration packages.
- `ParticleCommandRuntime` and `io.github.tt432.eyelibparticle.api.ParticleSpawnRequest` stay string/platform-type-free.
- `:eyelib-particle` pure sources must not import root runtime, root network, root capability, root `mc/impl`, Minecraft, or Forge classes.
- Do not add a new packet-contract module in Phase 13; PFUT-02 remains deferred.

## Recommended File Ownership By Plan

| Plan | Exclusive Runtime/Test Files |
|------|------------------------------|
| 13-01 | `ParticleCommandRuntime.java`, `EyelibParticleCommand.java`, `ParticleCommandRuntimeTest.java`, `EyelibParticleCommandBoundaryTest.java` |
| 13-02 | `SpawnParticlePacket.java`, `RemoveParticlePacket.java`, `EyelibNetworkTransport.java`, `NetClientHandlers.java`, `ParticleSpawnService.java`, network tests |
| 13-03 | `MODULES.md`, architecture docs, particle/network/command READMEs, documentation test |

## Pattern Mapping Complete

Use this map as executor context for Phase 13 plans.
