# Behavior Entity 改造方案：最小可行改造 (MVP)

**日期:** 2026-06-02  
**目标:** 让 `components` 字段从 importer 正确传播到 runtime，并展示完整链路  
**元问题:** GAP-BE-001 (components 丢失), GAP-BE-002 (dispatch 极窄), GAP-BE-010 (无测试)

---

## 1. 当前状态摘要

### 1.1 数据流现状

```
vanilla_behavior_pack/entities/*.json
        │
        ▼
  BrBehaviorEntityFile.parse()          ← importer 解析，components 作为 raw ObjectValue 保留 ✔
        │
        ▼
  BehaviorEntityAssetRegistry.toBehaviorEntity()
        │                                   ← ✗ 忽略 file.components()
        │                                   ← ▢ 仅提取 variant/mark_variant
        ▼
  BehaviorEntity(identifier, component_groups, events)
        │                                   ← ✗ 无 components 字段
        ▼
  EntityBehaviorData.setup()             ← 仅从 component_groups 构建
        │
        ▼
  EntityRenderSystem.onEvent()           ← 运行时消费，components 完全丢失
```

### 1.2 现有组件注册状态

| 组件 | 实现 Codec | 已注册 Dispatch | 运行时可用 |
|------|-----------|----------------|-----------|
| `minecraft:variant` | Variant.java | ✔ | ✔ |
| `minecraft:mark_variant` | MarkVariant.java | ✔ | ✔ |
| `minecraft:ageable` | Ageable.java | ✗ → EmptyComponent | ✗ |
| `minecraft:admire_item` | AdmireItem.java | ✗ → EmptyComponent | ✗ |
| `minecraft:addrider` | Addrider.java | ✗ → EmptyComponent | ✗ |
| `minecraft:physics` | Physics.java (空标记类) | ✗ | ✗ |
| `minecraft:ambient_sound_interval` | AmbientSoundInterval.java (空标记类) | ✗ | ✗ |
| ~200+ 其他组件 | — | ✗ → EmptyComponent | ✗ |

### 1.3 问题根因

1. **`BehaviorEntity` record 缺少 `components` 字段** — 所有顶层组件被桥接层静默丢弃
2. **`BehaviorEntityAssetRegistry.toBehaviorEntity()` 未读取 `file.components()`** — 即使 importer 正确解析
3. **`EntityBehaviorData.setup()` 仅从 `component_groups` 构建** — 顶层 components 无处流入
4. **`ComponentGroup.CODEC` dispatch 仅注册 2 个组件** — Ageable/AdmireItem/Addrider 有 codec 却未注册
5. **无任何测试** — `eyelib-behavior/src/test/` 不存在

---

## 2. 最小可行改造方案 (MVP)

### 原则

- **不改动 importer 层** — BrBehaviorEntityFile 的解析已经正确
- **只在 bridge + runtime 层做最小修改** — 让 components 字段流转起来
- **保持向后兼容** — 所有现有接口签名不变
- **展示完整链路** — 从 importer → bridge → runtime → 测试一条龙

### 2.1 新增 `BehaviorComponents` 记录 (runtime 层)

**文件:** `eyelib-behavior/src/main/java/.../eyelibbehavior/BehaviorComponents.java`

一个轻量 record，作为顶层 `components` 的 typed 容器：

```java
package io.github.tt432.eyelibbehavior;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibbehavior.component.Component;
import io.github.tt432.eyelibbehavior.component.EmptyComponent;
import io.github.tt432.eyelibbehavior.component.Health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 顶层 minecraft:entity.components 的 typed 存储。
 * 使用与 ComponentGroup 相同的 KeyDispatchMapCodec 分发策略。
 */
public record BehaviorComponents(
        Map<String, Component> components
) {
    public static final BehaviorComponents EMPTY = new BehaviorComponents(Collections.emptyMap());

    // CODEC 使用与 ComponentGroup 相同的 dispatch 表
    // 见 ComponentGroup.DISPATCH_CODEC
}
```

> **设计理由:** 保持与 `ComponentGroup` 一致的 codec 分发机制，不引入新的 dispatch 表。

### 2.2 修改 `BehaviorEntity` — 增加 `components` 字段

**文件:** `eyelib-behavior/src/main/java/.../eyelibbehavior/BehaviorEntity.java`

```diff
 public record BehaviorEntity(
         ResourceLocation identifier,
         Map<String, ComponentGroup> component_groups,
+        BehaviorComponents components,
         Map<String, LogicNode> events
 ) {
     public static final Codec<BehaviorEntity> CODEC = RecordCodecBuilder.create(instance -> instance.group(
             RecordCodecBuilder.<BehaviorEntity>create(instance2 -> instance2.group(
                     ResourceLocation.CODEC.fieldOf("identifier").forGetter(BehaviorEntity::identifier),
                     Codec.unboundedMap(Codec.STRING, ComponentGroup.CODEC).fieldOf("component_groups").forGetter(BehaviorEntity::component_groups),
+                    BehaviorComponents.CODEC.optionalFieldOf("components", BehaviorComponents.EMPTY).forGetter(BehaviorEntity::components),
                     Codec.unboundedMap(Codec.STRING, LogicNode.CODEC.codec()).optionalFieldOf("events", Collections.emptyMap()).forGetter(BehaviorEntity::events)
             ).apply(instance2, BehaviorEntity::new)).fieldOf("minecraft:entity").forGetter(e -> e)
     ).apply(instance, e -> e));
```

> **影响:** 这个修改在现有字段中间插入，不会破坏构造函数调用——重写对应构造函数即可。

### 2.3 修改 `BehaviorEntityAssetRegistry.toBehaviorEntity()` — 连接 bridge

**文件:** `src/main/java/.../client/registry/BehaviorEntityAssetRegistry.java`

在 `toBehaviorEntity()` 方法中增加对 `file.components()` 的解析：

```diff
 @Nullable
 private static BehaviorEntity toBehaviorEntity(BrBehaviorEntityFile file) {
     ResourceLocation identifier = ResourceLocation.tryParse(file.identifier());
     if (identifier == null) return null;

     // 原有 component_groups 解析保持不变
     var groups = new LinkedHashMap<String, ComponentGroup>();
     // ... (existing code) ...

+    // === 新增：解析顶层 components ===
+    BehaviorComponents topComponents = BehaviorComponents.EMPTY;
+    BedrockResourceValue.ObjectValue rawComponents = file.components();
+    if (rawComponents != null && !rawComponents.values().isEmpty()) {
+        var parsed = new LinkedHashMap<String, Component>();
+        for (var entry : rawComponents.values().entrySet()) {
+            String compKey = entry.getKey();
+            BedrockResourceValue compVal = entry.getValue();
+            Component component = parseSingleComponent(compKey, compVal);
+            if (component != null) {
+                parsed.put(compKey, component);
+            }
+        }
+        if (!parsed.isEmpty()) {
+            topComponents = new BehaviorComponents(parsed);
+        }
+    }
+    // === 新增结束 ===

     Map<String, LogicNode> events = parseEvents(file.events());

-    return new BehaviorEntity(identifier, groups, events);
+    return new BehaviorEntity(identifier, groups, topComponents, events);
 }

+ /**
+  * 解析单个行为实体顶层组件。
+  * 先尝试已知 typed 组件，未知组件用 RawComponent（保留原始 JSON）兜底。
+  */
+ @Nullable
+ private static Component parseSingleComponent(String key, BedrockResourceValue compVal) {
+     if (!(compVal instanceof BedrockResourceValue.ObjectValue obj)) {
+         return null;
+     }
+     // 使用与 ComponentGroup 相同的 dispatch 策略
+     return switch (key) {
+         case "minecraft:variant" -> parseVariant(obj);
+         case "minecraft:mark_variant" -> parseMarkVariant(obj);
+         case "minecraft:health" -> parseHealth(obj);
+         // 可扩展：新组件在这里加 case
+         default -> {
+             // 未知组件保留原始数据，不丢失
+             yield new RawComponent(key, obj);
+         }
+     };
+ }
```

### 2.4 新增 `RawComponent` — 未知组件的兜底存储

**文件:** `eyelib-behavior/src/main/java/.../eyelibbehavior/component/RawComponent.java`

```java
package io.github.tt432.eyelibbehavior.component;

import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;

/**
 * 兜底组件，保留 importer 层的原始 BedrockResourceValue 数据。
 * 当组件尚未有 typed codec 时使用，保证数据不丢失。
 */
public record RawComponent(
        String componentId,
        BedrockResourceValue.ObjectValue rawData
) implements Component {
    @Override
    public String id() {
        return "raw:" + componentId;
    }
}
```

> **设计理由:** 替代现有的 `EmptyComponent` 模式。`EmptyComponent` 丢弃所有数据，`RawComponent` 保留原始 JSON 供后续消费或调试。

### 2.5 新增 `Health` 组件 — 示例新组件

**文件:** `eyelib-behavior/src/main/java/.../eyelibbehavior/component/Health.java`

```java
package io.github.tt432.eyelibbehavior.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:health — 实体生命值定义。
 * Bedrock 规范: { "value": int, "max": int }
 */
public record Health(
        int value,
        int max
) implements Component {
    public static final Codec<Health> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("value").forGetter(Health::value),
            Codec.INT.optionalFieldOf("max", 20).forGetter(Health::max)
    ).apply(ins, Health::new));

    @Override
    public String id() {
        return "health";
    }
}
```

### 2.6 修改 `EntityBehaviorData.setup()` — 合并顶层 components

**文件:** `eyelib-behavior/src/main/java/.../eyelibbehavior/EntityBehaviorData.java`

```diff
 public void setup() {
     components.clear();
+    // 1. 从 component_groups 收集
     for (ComponentGroup componentGroup : componentGroups) {
         for (Map<String, Component> value : componentGroup.components().values()) {
             value.values().forEach(component -> components.put(component.getClass(), component));
         }
     }
+    // 2. 从 behavior 的顶层 components 收集（覆盖优先级低）
+    behavior.ifPresent(b -> {
+        for (Component component : b.components().components().values()) {
+            // 记录但不覆盖 — component_groups 中的组件优先级更高
+            components.putIfAbsent(component.getClass(), component);
+        }
+    });
 }
```

### 2.7 将已实现组件注册到 `ComponentGroup.CODEC`

**文件:** `eyelib-behavior/src/main/java/.../eyelibbehavior/component/group/ComponentGroup.java`

```diff
 public static final Codec<ComponentGroup> CODEC = Codec.unboundedMap(Codec.STRING, new KeyDispatchMapCodec<>(Codec.STRING, s -> switch (new ResourceLocation(s).toString()) {
     case "minecraft:variant" -> Variant.CODEC;
     case "minecraft:mark_variant" -> MarkVariant.CODEC;
+    case "minecraft:ageable" -> Ageable.CODEC;
+    case "minecraft:admire_item" -> AdmireItem.CODEC;
+    case "minecraft:addrider" -> Addrider.CODEC;
+    case "minecraft:health" -> Health.CODEC;
     default -> new Codec<Component>() {
         @Override
         public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
-            log.error("Unknown component type: {}", s);
+            log.warn("Unknown component type: {}, using raw fallback", s);
-            return DataResult.success(new Pair<>(EmptyComponent.INSTANCE, input));
+            return DataResult.success(new Pair<>(new RawComponent(s, /* need JsonElement here via ops */), input));
         }
         // ...
     };
 }));
```

> **注意:** `RawComponent` 的 fallback 实现需要从 `DynamicOps` 反序列化回 `JsonElement`，这在 codec 内部稍复杂。**MVP 简化方案:** fallback 仍然返回 `EmptyComponent.INSTANCE`，但顶层 bridge 的 `parseSingleComponent` 用 `RawComponent` 兜底。codec 内部的 raw fallback 列为下一阶段优化。

---

## 3. 文件修改清单

| # | 操作 | 文件 | 修改类型 | 复杂度 |
|---|------|------|---------|--------|
| 1 | 新增 | `eyelib-behavior/.../component/Health.java` | 新建 | 简单 (30行) |
| 2 | 新增 | `eyelib-behavior/.../component/RawComponent.java` | 新建 | 简单 (20行) |
| 3 | 新增 | `eyelib-behavior/.../BehaviorComponents.java` | 新建 | 简单 (40行) |
| 4 | 修改 | `eyelib-behavior/.../BehaviorEntity.java` | 增加 field + codec | 中等 (5行) |
| 5 | 修改 | `eyelib-behavior/.../EntityBehaviorData.java` | setup() 合并逻辑 | 简单 (8行) |
| 6 | 修改 | `eyelib-behavior/.../component/group/ComponentGroup.java` | 注册新组件 | 简单 (4行) |
| 7 | 修改 | `src/.../registry/BehaviorEntityAssetRegistry.java` | bridge 增加 components 解析 | 中等 (40行) |
| 8 | 修改 | `eyelib-behavior/src/test/.../BehaviorEntityComponentsTest.java` | 新建测试 | 中等 (100行) |
| 9 | 修改 | `eyelib-behavior/src/test/.../BehaviorEntityBridgeIntegrationTest.java` | 新建集成测试 | 中等 (120行) |

---

## 4. 测试设计

### 4.1 单元测试: `BehaviorEntityComponentsTest`

**文件:** `eyelib-behavior/src/test/java/io/github/tt432/eyelibbehavior/BehaviorEntityComponentsTest.java`

```java
// 测试 1: BehaviorComponents 空构造
void emptyComponents() {
    var bc = BehaviorComponents.EMPTY;
    assertTrue(bc.components().isEmpty());
}

// 测试 2: Health 组件 codec 解析
void healthComponentCodec() {
    var json = """
        { "value": 20, "max": 20 }
        """;
    var parsed = Health.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
            .getOrThrow(false, s -> {});
    assertEquals(20, parsed.value());
    assertEquals(20, parsed.max());
}

// 测试 3: Health 组件默认值 (max 不提供)
void healthComponentDefaultMax() {
    var json = """ { "value": 10 } """;
    var parsed = Health.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
            .getOrThrow(false, s -> {});
    assertEquals(10, parsed.value());
    assertEquals(20, parsed.max());  // 默认值
}

// 测试 4: RawComponent 保留原始数据
void rawComponentPreservesData() {
    var raw = new RawComponent("minecraft:is_baby", someObjectValue);
    assertEquals("minecraft:is_baby", raw.componentId());
    assertNotNull(raw.rawData());
}
```

### 4.2 集成测试: `BehaviorEntityBridgeIntegrationTest`

**文件:** `eyelib-behavior/src/test/java/io/github/tt432/eyelibbehavior/BehaviorEntityBridgeIntegrationTest.java`

```java
// 测试 5: 完整 JSON → BrBehaviorEntityFile → BehaviorEntity 链路
void fullParsePipelineWithComponents() {
    var json = """
        {
            "format_version": "1.20.0",
            "minecraft:entity": {
                "description": { "identifier": "test:health_entity" },
                "component_groups": {},
                "components": {
                    "minecraft:health": { "value": 30, "max": 30 },
                    "minecraft:variant": { "value": 2 },
                    "minecraft:unknown_test": { "some_field": true }
                },
                "events": {}
            }
        }
        """;

    // 1. importer 解析
    BrBehaviorEntityFile file = BrBehaviorEntityFile.parse(JsonParser.parseString(json).getAsJsonObject());
    assertNotNull(file.components());
    assertEquals("test:health_entity", file.identifier());

    // 2. bridge 转换（直接调用内部的 toBehaviorEntity）
    // 由于该方法是 private，通过反射或复制逻辑测试
    // 或者改为 package-private 后在测试中调用
    BehaviorEntity entity = bridgeToBehaviorEntity(file);
    assertNotNull(entity);

    // 3. 验证 components
    Component health = entity.components().components().get("minecraft:health");
    assertTrue(health instanceof Health);
    assertEquals(30, ((Health) health).value());

    Component variant = entity.components().components().get("minecraft:variant");
    assertTrue(variant instanceof Variant);
    assertEquals(2, ((Variant) variant).value());

    // 4. 未知组件保留为 RawComponent
    Component unknown = entity.components().components().get("minecraft:unknown_test");
    assertTrue(unknown instanceof RawComponent);
}

// 测试 6: EntityBehaviorData.setup() 合并顶层 components
void setupMergesTopLevelComponents() {
    var entity = createTestEntity(); // 包含 minecraft:health
    var data = new EntityBehaviorData(Optional.of(entity), new ArrayList<>());
    data.setup();

    Health health = data.component(Health.class);
    assertNotNull(health);
    assertEquals(30, health.value());
}
```

### 4.3 /eval 集成测试案例 (用于在 AI Debug Server 验证)

**请求体:**
```java
import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelibbehavior.BehaviorEntity;
import io.github.tt432.eyelibbehavior.BehaviorComponents;
import io.github.tt432.eyelibbehavior.component.Health;
import io.github.tt432.eyelibbehavior.component.Component;
import io.github.tt432.eyelibbehavior.Variant;

var mgr = BehaviorEntityManager.INSTANCE;

// 找到一个已加载的实体（例如 minecraft:zombie）
var zombie = mgr.get("minecraft:zombie");
if (zombie == null) return "ZOMBIE_NOT_FOUND";

// 确认 BehaviorEntity 已有 components 字段
var recordComponents = zombie.getClass().getRecordComponents();
java.util.List<String> names = new java.util.ArrayList<>();
for (var rc : recordComponents) names.add(rc.getName());
boolean hasComponents = names.contains("components");

// 统计顶层 components 数量
int topCount = zombie.components().components().size();

// 统计其中 Health 组件的数量
long healthCount = zombie.components().components().values().stream()
    .filter(c -> c instanceof Health)
    .count();

// 统计 RawComponent 兜底数量
long rawCount = zombie.components().components().values().stream()
    .filter(c -> c.getClass().getSimpleName().equals("RawComponent"))
    .count();

return "hasComponents=" + hasComponents
    + ", topLevelComponents=" + topCount
    + ", healthComponents=" + healthCount
    + ", rawFallbacks=" + rawCount;
```

**预期结果:** `hasComponents=true, topLevelComponents=N, healthComponents=N, rawFallbacks=N`

---

## 5. 实施顺序

```
Phase 1 (P0 — 核心链路贯通)
├── 1.1 新增 Health.java + RawComponent.java + BehaviorComponents.java
├── 1.2 修改 BehaviorEntity.java (增加 components 字段)
├── 1.3 修改 BehaviorEntityAssetRegistry.java (bridge 层解析)
├── 1.4 修改 EntityBehaviorData.setup() (合并逻辑)
├── 1.5 修改 ComponentGroup.CODEC (注册已有组件 + Health)
└── 1.6 新建测试 + 运行 /eval 验证

Phase 2 (P1 — 覆盖扩展)
├── 2.1 注册 Ageable/AdmireItem/Addrider
├── 2.2 实现 BitFlag 组件通用方案 (is_baby, is_tamed 等)
├── 2.3 添加更多 typed 组件 (scale, collision_box, movement 等)
└── 2.4 将 ComponentGroup codec 内部 fallback 也改为 RawComponent

Phase 3 (P2 — 完整覆盖)
├── 3.1 常见 behavior goal 组件
├── 3.2 Description typed 建模
├── 3.3 事件系统扩展 (trigger, filters, set_property)
└── 3.4 Spawn rules runtime 桥接
```

---

## 6. 风险与注意事项

1. **`BehaviorEntity` record 构造函数变化**: 在 identifier/component_groups 之间插入 components 字段。如果已有代码直接调用 `new BehaviorEntity(...)`，需要更新参数顺序。建议使用 `@Builder` 或提供旧签名兼容工厂方法。

2. **`BehaviorEntity.CODEC` 向后兼容**: `components` 字段使用 `optionalFieldOf`，旧 JSON 文件（无 `components` 字段）仍然可以解析。

3. **`RawComponent` 的 codec 序列化**: 在 codec 内部的 fallback 中使用 `RawComponent` 需要将 `DynamicOps` 的数据转换为 `BedrockResourceValue`。这需要额外的 ops-to-json 转换逻辑，MVP 阶段 codec 内部继续用 `EmptyComponent`，仅在 bridge 层使用 `RawComponent`。

4. **线程安全**: `EntityBehaviorData.setup()` 目前没有同步。如果多线程调用需要加锁。

5. **测试目录**: `eyelib-behavior/src/test/` 不存在，需要创建 `src/test/java/io/github/tt432/eyelibbehavior/` 目录结构并添加 `build.gradle` 的测试依赖（JUnit 5, AssertJ 等）。
