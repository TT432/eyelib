# 行为组件实现陷阱

> behavior 模块组件实现中发现的 CODEC 陷阱和修复记录。

## DFU RecordCodecBuilder.group() 最多 16 个类型参数

**症状**: 编译错误 `对于group(...), 找不到合适的方法`，当 record 有超过 16 个字段时触发。

**根因**: DFU 的 `RecordCodecBuilder.group(App<F,T1>, ..., App<F,T16>)` 方法重载最多支持 16 个类型参数。

**修复**: 将字段按语义分成 2-3 个内部 record，每组合 ≤16 字段，各用独立 MapCodec。主 CODEC 用这些 MapCodec 组合。

**受影响的组件**: `Projectile.java` (32 字段) — 拆为 `ProjectilePhysics`(8), `ProjectileCombat`(8), `ProjectileBehavior`(16) 三个 MapCodec。

## Codec.JSON_ELEMENT 不存在

**症状**: 编译错误 `找不到符号: JSON_ELEMENT`。

**根因**: DFU 的 `com.mojang.serialization.Codec` 没有 `JSON_ELEMENT` 字段。子代理可能编造此 API。

**修复**:
```java
private static final Codec<JsonObject> JSON_FIELD = Codec.STRING.xmap(
    JsonParser::parseString,
    JsonElement::toString
).xmap(e -> e.getAsJsonObject(), o -> o);
```

**受影响的组件**: `Healable.java`, `Interact.java`, `LookedAt.java`, `MobEffect.java`（共 4 文件）。

## Marker 组件必须包含 INSTANCE 和 CODEC

**症状**: 编译错误 `找不到符号: CODEC`。

**根因**: 子代理创建标记组件（空 record）时可能省略 `INSTANCE` 和 `CODEC` 字段。

**正确模式**:
```java
@org.jspecify.annotations.NullMarked
public record Xxx() implements Component {
    public static final Xxx INSTANCE = new Xxx();
    public static final Codec<Xxx> CODEC = io.github.tt432.eyelib.util.codec.EyelibCodec.unit(INSTANCE);
    @Override public String id() { return "xxx"; }
}
```

**受影响的组件**: `InsideBlockNotifier.java`, `SuspectTracking.java`, `VibrationDamper.java`, `VibrationListener.java`（共 4 文件）。

## 旧骨架类与新 record 命名冲突

**症状**: 编译错误 `对Physics的引用不明确`。

**根因**: 旧模块 `component/Physics.java`（空 class，未实现 Component）与新 `component/property/Physics.java`（record，实现 Component）同名。

**修复**: 删除旧骨架文件。保留新 record。

**受影响的文件**: `component/Physics.java`（删除），`component/AmbientSoundInterval.java`（删除，迁移到 property/）。

## Bedrock filter 字段必须用 optionalFieldOf

**症状**: `No key filters in MapLike[...]` — Bedrock JSON 不含 `filters` 字段时 CODEC 解析失败。

**根因**: 传感器的 filter 字段在 Bedrock 中总是可选的。用 `fieldOf`（必填）导致解析失败。

**修复**: `JSON_OBJECT_CODEC.optionalFieldOf("filters", new JsonObject()).forGetter(...)`。

**受影响的组件**: `EnvironmentSensor.java`, `BlockSensor.java`, `HurtOnCondition.java`, `Scheduler.java`, `SpawnEntity.java`, `EntitySensor.java`（2 处）— 共 7 处。

## EventRef 必须用 optionalFieldOf + sentinel

**症状**: 运行时崩溃 `No key on_leash in MapLike[...]` — Bedrock JSON 常省略事件字段。

**根因**: `EventRef.CODEC.fieldOf("on_leash")` 要求 JSON 中必须存在事件字段，但 Bedrock 中事件字段总是可选的。

**修复**:
1. 在 EventRef 中添加 sentinel: `public static final EventRef NONE = new EventRef("", "self");`
2. 所有使用处改为: `EventRef.CODEC.optionalFieldOf("on_leash", EventRef.NONE).forGetter(...)`

**受影响的组件**: `Leashable.java`, `Balloonable.java`, `Giveable.java`, `Equippable.java`, `Interact.java`, `LookedAt.java`, `Sittable.java`, `Tameable.java`（共 8 文件，12 处）。

## BehaviorEntityAssetRegistry 手动解析与 DISPATCH_CODEC 冲突

**症状**: `ClassCastException: RawComponent cannot be cast to Attack` — 实体 NBT 保存时崩溃。

**根因**: `BehaviorEntityAssetRegistry` 的手动解析路径创建 RawComponent 但存在原始 Bedrock key（如 `"minecraft:attack"`）下。DISPATCH_CODEC encode 时 dispatch 到 typed CODEC（如 `Attack.CODEC`），但实际对象是 RawComponent → CCE。

**修复**: CODEC 失败的组件返回 null（跳过），不存 RawComponent。后续修正 CODEC 字段可选性后组件会自动重新识别。手动解析的 `parseSingleComponent` 用 try-catch 包裹 CODEC dispatch 路径。

## EyelibCodec.list() 双层 fieldOf bug（eyelib-util 模块，2026-06-08 已修复）

**症状**: `LogicNode.CODEC` 解析 events JSON 时始终失败，events map 返回空。`ComplexFilter.CODEC` 同样受影响。

**根因**: `eyelib-util` 中 `EyelibCodec.list()` line 81 用 `v.codec.fieldOf(k)` 创建已包裹 key 的 MapCodec，但 line 111 又调用 `codec.fieldOf(p.getFirst())` — 形成双层 `fieldOf` 包裹。JSON 需要 `{"add": {"add": {...}}}` 而非正确的 `{"add": {...}}`。

**修复**（`EyelibCodec.java` line 111）:
```java
// 修复前（双层包裹）
return codec.fieldOf(p.getFirst()).decode(ops, input);
// 修复后（codec 已含 fieldOf(k)，直接 decode 即可）
return codec.decode(ops, input);
```

**为什么正确**: line 81 创建的 `codec` 已经是 `Add.CODEC.fieldOf("add")`，它知道要查找 key `"add"` 并提取值。line 111 再包一层 `fieldOf("add")` 导致查找 `"add"` 下再查找 `"add"`。移除后 `codec.decode(ops, input)` 直接在 input map 中查找自己的 key。

**影响面**: `LogicNode.CODEC`（LogicNode.java line 17）和 `ComplexFilter.CODEC`（ComplexFilter.java line 21）——两个使用 `EyelibCodec.list()` 的地方。`BehaviorEntity.CODEC` 也间接受益（它内部用 `LogicNode.CODEC.codec()`）。

**验证**: 修复后 `BehaviorEntityAssetRegistry.parseEvents()` 改用 `LogicNode.CODEC.codec()` 替代手动解析，全量 121 实体 event 加载成功（561 个事件，5 种类型全部出现）。

**相关变更**: 同 session 中 `BehaviorEntityAssetRegistry` 的 `component_groups` 解析也从手动 variant/mark_variant 切到 `DISPATCH_CODEC`；`parseEvents` 从手动 switch 切到 `LogicNode.CODEC.codec()`。两处旧手动解析路径已删除。
