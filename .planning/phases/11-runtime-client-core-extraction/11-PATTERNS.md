---
phase: 11-runtime-client-core-extraction
created: 2026-05-09
status: complete
---

# Phase 11 Pattern Map

## Closest Existing Patterns

| Concern | Existing Analog | Pattern to Reuse |
|---------|-----------------|------------------|
| Module boundary docs | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` | State scope, current responsibilities, dependency direction, integration rule, consumers, verification rule. |
| Pure subproject boundary test | `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java` | Walk source tree, strip comments/string literals, reject forbidden fragments. |
| Root facade delegation test | `src/test/java/io/github/tt432/eyelib/client/particle/ParticleSpawnServiceBoundaryTest.java` | Source scan for imports/delegation strings and forbidden root contamination. |
| Forge hook quarantine | `src/main/java/io/github/tt432/eyelib/mc/impl/capability/CapabilityComponentRuntimeHooks.java` | Dedicated `@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = ...)` hook class delegates to domain methods. |
| Loader lifecycle hook | `src/main/java/io/github/tt432/eyelib/mc/impl/client/loader/ClientLoaderLifecycleHooks.java` | Event hook is only wiring; concrete behavior stays in explicit services. |
| Runtime manager lifecycle | `BrParticleRenderManager` current behavior | Preserve methods `spawnEmitter`, `removeEmitter`, `spawnParticle`, cleanup and tick/render order while moving ownership. |

## Current Runtime Excerpts

### Render manager event contract

From `src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticleRenderManager.java`:

```java
@SubscribeEvent
public static void onEvent(TickEvent.RenderTickEvent event) {
    if (event.phase != TickEvent.Phase.START) return;
    emitters.object2ObjectEntrySet().removeIf(removeEmitters);
    emitters.values().forEach(renderEmitters);
    particles.removeIf(removeParticles);
    particles.forEach(renderParticles);
}

@SubscribeEvent
public static void onEvent(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.START) return;
    emitters.values().forEach(BrParticleEmitter::onTick);
}

@SubscribeEvent
public static void onEvent(RenderLevelStageEvent event) {
    if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
        RenderTypeResolver.EntityRenderTypeData factory = RenderTypeResolver.resolve(new ResourceLocation(material));
        var buffer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(
                factory.factory().apply(particle.getTexture().withSuffix(".png"))
        );
        particle.render(poseStack, buffer);
    }
}
```

### Emitter Molang/runtime contract

From `BrParticleEmitter.java`:

```java
molangScope.setParent(parentScope);
molangScope.getHostContext().put(BrParticleEmitter.class, this);
particle.particleEffect().curves().forEach((k, v) -> molangScope.set(k, () -> v.calculate(molangScope)));
molangScope.set("variable.emitter_age", this::getAge);
molangScope.set("variable.emitter_lifetime", this::getLifetime);
molangScope.set("variable.emitter_random_1", this::getRandom1);
...
components.forEach(c -> c.onPreTick(this));
components.forEach(c -> c.onTick(this));
```

### Particle render contract

From `BrParticleParticle.java`:

```java
molangScope.setParent(emitter.molangScope);
molangScope.getHostContext().put(BrParticleParticle.class, this);
emitter.getParticle().particleEffect().curves().forEach((k, v) -> molangScope.set(k, () -> v.calculate(molangScope)));
molangScope.set("variable.particle_age", this::getAge);
molangScope.set("variable.particle_lifetime", this::getLifetime);
...
int light = lighting != null ? LightTexture.FULL_BRIGHT : LightTexture.pack(...);
vertexConsumer.vertex(... OverlayTexture.NO_OVERLAY, light, normal.x, normal.y, normal.z);
```

## Target Package Pattern

- Pure runtime/core: `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/**`
  - May depend on `io.github.tt432.eyelibparticle.*`, `io.github.tt432.eyelibimporter.*`, `io.github.tt432.eyelibmolang.*`, JOML, Java collections.
  - Must not depend on `io.github.tt432.eyelib.client.*`, `io.github.tt432.eyelib.mc.impl.*`, `net.minecraft.*`, or `net.minecraftforge.*`.
- Client integration: `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/client/**`
  - May own `Minecraft`, `Level`, `Entity`, `PoseStack`, `VertexConsumer`, `RenderLevelStageEvent`, `TickEvent`, `ClientPlayerNetworkEvent`, and `@Mod.EventBusSubscriber` usage.
  - Must be `Dist.CLIENT` side-safe and documented as integration, not pure runtime.
- Root compatibility: `src/main/java/io/github/tt432/eyelib/client/particle/**`
  - Transitional only; delegates into particle module APIs/services and documents later removal conditions.

## Source Audit

| Source | Item | Coverage |
|--------|------|----------|
| GOAL | Runtime/client rendering behavior lives under `:eyelib-particle` without weakened side boundaries | Plans 01-06 |
| REQ PRENDER-01 | Emitter, render manager, material/texture, Molang, lifetime, remove, tick/render, logout behavior preserved | Plans 02-06 |
| REQ PRENDER-02 | Client-only hooks and platform integrations side-safe | Plans 01, 05, 06 |
| CONTEXT D-01 | Do not move loading/publication/command/network contract behavior | Plans 05-06 explicitly preserve/defer |
| CONTEXT D-02 | Preserve current bedrock runtime behavior | Plans 02-05 |
| CONTEXT D-03 | ParticleDefinition seam remains canonical; no module BrParticle duplicate | Plans 01, 02, 04 |
| CONTEXT D-04 | Small staged extraction; root classes transitional | Plans 01-06 |
| CONTEXT D-05-D-08 | Pure core clean, explicit client integration, side-safe adapters, string identifiers | Plans 01, 05 |
| CONTEXT D-09-D-11 | Root facades delegate; packet spawn/remove behavior compatible | Plan 06 |
| CONTEXT D-12-D-14 | Hook timing and render material/texture semantics preserved | Plan 05 |
| CONTEXT D-15-D-17 | JetBrains MCP checks, boundary/side/delegation/runtime semantics, manual visual deferred | Validation + all plans |
| Deferred | Phase 12 loading/publication, Phase 13 command/network rewire, Phase 14 broad verification | Excluded from plans except compatibility preservation |
