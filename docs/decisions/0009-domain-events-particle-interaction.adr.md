# ADR-0009: Domain Events — Particle Interaction 解耦

**Status:** Implemented (Phase 1: Interface boundary)  
**Date:** 2026-06-01  
**Context:** Phase 3 DDD 重构目标：消除 `eyelib-animation` 模块对 `eyelib-particle` 模块内部类型的直接编译期依赖，引入 Domain Events 作为交互边界。  

**Decision:**  
将 `AnimationParticleSpawner` 从具体实现类重构为纯接口，接口签名只传递 string 粒度的 `effectId`（shortname），不再传递 `ParticleDefinition` 和 `BedrockParticleEmitter` 等 particle 内部类型。同时在以下位置消除对 `ParticleDefinitionRegistry`、`ParticleDefinition`、`BedrockParticleEmitter` 的直接 import：

1. **`eyelib-animation/build.gradle`** — `implementation project(':eyelib-particle')` 改为 `compileOnly`（或移除，如无其他编译期引用）
2. **`AnimationParticleSpawner`** — 从 final class 改为接口
3. **`RuntimeParticlePlayData`** — 去掉 `emitter` 字段（死存储，只写不读）
4. **`BrControllerExecutor`** — 不再 import `ParticleDefinitionRegistry`
5. **`BrAnimationEntryDefinition`** — 不再 import `ParticleDefinitionRegistry`

Particle runtime 侧的解析（`effectId → ParticleDefinition`）由 particle 模块内部的事件消费者负责完成。

**Consequences:**  
- Animation 模块不再持有 particle 内部类型的引用，边界清晰
- `RuntimeParticlePlayData` 的 record 定义简化
- 需要新增一个 particle 侧的 Domain Event subscriber 来监听 spawn 请求并执行 `ParticleDefinitionRegistry.store().get(effectId)` 转换
- 已有的 `BrControllerExecutor.switchState()` 和 `BrAnimationEntryDefinition.particleEffect()` 中内联的 registry lookup 逻辑需要迁移到 subscriber 中

---

# 设计细节

## 1. Bedrock 原版参考

Bedrock 原版设计中，animation 侧只处理 **shortname**（如 `"fuse_lit"`），不接触完整粒子 identifier。解析链如下：

```
客户端实体文件 particle_effects 映射
    shortname → identifier (e.g. "fuse_lit" → "minecraft:fuse_lit")

Animation / Animation Controller 触发时只传 shortname
    → particle runtime 负责 identifier → 实际粒子定义
```

当前代码中 animation 模块做了本不该做的 registry lookup，这是边界违反的核心。

## 2. 接口变更

### `AnimationParticleSpawner`：改前

```java
// eyelib-animation/src/main/java/.../AnimationParticleSpawner.java (before)
public final class AnimationParticleSpawner {
    private final ParticleSpawnRuntimeAdapter adapter;
    private final ParticleRuntimeEnvironment environment;

    public AnimationParticleSpawner(ParticleRuntimeEnvironment environment) { ... }

    @Nullable
    public BedrockParticleEmitter spawn(String spawnId, ParticleDefinition definition,
                                         Vector3f position) { ... }

    public void remove(String spawnId) { ... }
}
```

改前 import 依赖：`ParticleRenderManager`、`ParticleSpawnRuntimeAdapter`、`ParticleDefinitionRegistry`、`ParticleDefinition`、`BedrockParticleEmitter`、`ParticleRuntimeEnvironment`。

### `AnimationParticleSpawner`：改后

```java
// eyelib-animation/src/main/java/.../AnimationParticleSpawner.java (after)
public interface AnimationParticleSpawner {
    /**
     * @param spawnId  唯一标识此次 spawn 的 ID（用于后续 remove）
     * @param effectId 粒子 shortname（如 "fuse_lit"），由调用方从 client_entity 映射中获取
     * @param position 生成位置
     * @return true 表示请求已发送给 particle runtime（不代表一定成功 spawn）
     */
    boolean spawn(String spawnId, String effectId, Vector3f position);

    void remove(String spawnId);
}
```

改后 import 依赖：仅 `Vector3f`（来自 JOML，已是间接依赖）。所有 particle 内部类型引用被移除。

### `RuntimeParticlePlayData`：改前

```java
// eyelib-animation/src/main/java/.../RuntimeParticlePlayData.java (before)
public record RuntimeParticlePlayData(
        String particleUUID,
        BedrockParticleEmitter emitter,   // ← 死存储：只写不读
        @Nullable String locator,
        float startTicks
) { }
```

### `RuntimeParticlePlayData`：改后

```java
// eyelib-animation/src/main/java/.../RuntimeParticlePlayData.java (after)
public record RuntimeParticlePlayData(
        String particleUUID,
        @Nullable String locator,
        float startTicks
) { }
```

移除了 `BedrockParticleEmitter emitter` 字段及其 import。

### 调用点变更

#### `BrControllerExecutor.switchState()`（≈L86–L95）

**改前：**
```java
ParticleDefinition definition = ParticleDefinitionRegistry.store().get(effect);
if (definition != null) {
    AnimationParticleSpawner spawner = scope.getHostContext().get(AnimationParticleSpawner.class).orElse(null);
    if (spawner != null) {
        BedrockParticleEmitter emitter = spawner.spawn(uuid, definition, entity.position().toVector3f());
        if (emitter != null) {
            data.owner().particles().add(new RuntimeParticlePlayData(uuid, emitter, ..., ticks));
        }
    }
}
```

**改后：**
```java
// ParticleDefinitionRegistry.store().get(effect) 迁移到 particle 侧 subscriber
// 不再需要 emitter 返回值判断
if (spawner != null) {
    spawner.spawn(uuid, effect, entity.position().toVector3f());  // ← 直接传 shortname
    data.owner().particles().add(new RuntimeParticlePlayData(uuid, ..., ticks));
}
```

#### `BrAnimationEntryDefinition.particleEffect()`（≈L98–L115）

**改前：**
```java
ParticleDefinition definition = ParticleDefinitionRegistry.store().get(s);
if (definition != null) {
    AnimationParticleSpawner spawner = ...;
    if (spawner != null) {
        BedrockParticleEmitter emitter = spawner.spawn(uuid, definition, ...);
        if (emitter != null) {
            animationData.owner().particles().add(new RuntimeParticlePlayData(uuid, emitter, ..., ticks));
        }
    }
}
```

**改后：**
```java
if (spawner != null) {
    spawner.spawn(uuid, s, entity.position().toVector3f());  // ← 直接传 shortname
    animationData.owner().particles().add(new RuntimeParticlePlayData(uuid, ..., ticks));
}
```

## 3. 实现记录

### Phase 1（2026-06-02）— Interface Boundary
已完成的变更：
- `AnimationParticleSpawner` → pure interface，零 particle 内部类型依赖
- `RuntimeParticlePlayData` — 移除 `BedrockParticleEmitter emitter` 字段
- `BrControllerExecutor` / `BrAnimationEntryDefinition` — 移除所有 particle import，使用 string effectId 调用
- `eyelib-animation/build.gradle` — 移除 `implementation project(':eyelib-particle')`
- 根模块 `EntityRenderSystem.setParticlesPosition()` — 移除死代码（依赖已删除的 emitter 字段）

### Deferred（Phase 2 候选）
- Domain Event subscriber 在 particle 侧的独立实现
- `AnimationParticleSpawner` 的根模块 concrete 实现（等 animation → particle 的 host context 注入链路激活时再做）

## 4. ~~Domain Event 机制~~

引入一个轻量级的 Domain Event 总线（或直接使用已有的 `MolangScope.getHostContext()` + 事件分发模式），注册以下事件：

| Event | Producer (animation side) | Consumer (particle side) |
|-------|--------------------------|--------------------------|
| `ParticleSpawnRequest(spawnId, effectId, position)` | `BrControllerExecutor` / `BrAnimationEntryDefinition` | Particle runtime subscriber |
| `ParticleRemoveRequest(spawnId)` | `BrControllerExecutor.switchState()` | Particle runtime subscriber |

Particle 侧的 subscriber 负责：
1. 接收 `effectId` (shortname)
2. 查询 `ParticleDefinitionRegistry.store().get(effectId)` 获取 `ParticleDefinition`
3. 调用 `ParticleSpawnRuntimeAdapter.spawnEmitter(...)` 生成实际 emitter
4. 不需要向 animation 侧返回 `BedrockParticleEmitter` 引用（因为 animation 侧不再读取它）

## 4. 受影响文件清单

| 文件 | 变更类型 |
|------|---------|
| `eyelib-animation/build.gradle` | 将 `implementation project(':eyelib-particle')` 改为 `compileOnly` 或移除 |
| `eyelib-animation/src/.../AnimationParticleSpawner.java` | final class → interface；签名简化 |
| `eyelib-animation/src/.../RuntimeParticlePlayData.java` | 移除 `emitter` 字段 |
| `eyelib-animation/src/.../bedrock/controller/BrControllerExecutor.java` | 移除 `ParticleDefinitionRegistry`/`ParticleDefinition`/`BedrockParticleEmitter` import；传 shortname |
| `eyelib-animation/src/.../bedrock/BrAnimationEntryDefinition.java` | 移除 `ParticleDefinitionRegistry`/`ParticleDefinition`/`BedrockParticleEmitter` import；传 shortname |
| `eyelib-particle` (新文件) | 新增 Domain Event subscriber，负责 shortname → particle 的 lookup 和 spawn |

## 5. 验证标准

- [x] `BrControllerExecutor.java` 不 import `io.github.tt432.eyelibparticle.*` 包下的任何类
- [x] `BrAnimationEntryDefinition.java` 不 import `io.github.tt432.eyelibparticle.*` 包下的任何类
- [x] `RuntimeParticlePlayData` 不引用 `BedrockParticleEmitter`
- [x] `AnimationParticleSpawner` 是纯接口，无 particle 内部类型依赖
- [x] `eyelib-animation` 的 `build.gradle` 不包含 `implementation project(':eyelib-particle')`
- [ ] Particle runtime 的 Domain Event subscriber 可独立测试（Phase 2）
- [ ] 原功能行为不变：粒子 spawn/remove 仍然正常工作（需 host context 注入链路激活后验证）
