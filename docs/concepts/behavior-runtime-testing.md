# 行为组件运行时测试

> behavior 模块的运行时验证方法论和常见问题诊断。

## 核心原则

**CODEC 字段可选性的正确性只能在真实 .mcpack 数据上验证。** 单元测试无法暴露 Bedrock JSON 实际包含哪些字段——schema 说 optional 的字段，单元测试加了这些字段就能过，但真实数据可能不含这些字段。

## 验证流程

1. **编译** — 确保所有组件 CODEC 编译通过
2. **启动客户端** — `eyelib_debug_launch()`
3. **进入世界** — `eyelib_debug_enter_world(world_name="BehTestX")`
4. **运行时检查** — `/eval` 查询 BehaviorEntityManager 状态

### 诊断命令

```java
// 总览
java.util.LinkedHashMap all = (java.util.LinkedHashMap) 
    io.github.tt432.eyelib.client.manager.BehaviorEntityManager.INSTANCE.getAllData();
// all.size() → 实体数

// 具体实体检查
io.github.tt432.eyelib.behavior.BehaviorEntity entity = 
    (io.github.tt432.eyelib.behavior.BehaviorEntity) all.get("minecraft:creeper");
entity.components().components().size(); // 组件数
entity.component_groups().size();        // 组数
entity.events().size();                  // 事件数
```

## 常见崩溃模式

### 1. CODEC 字段缺失 (fieldOf vs optionalFieldOf)

**崩溃信息**: `No key <field> in MapLike[...]`

**根因**: CODEC 用 `fieldOf`（必填）但 Bedrock JSON 缺该字段。

**修复**: 改为 `optionalFieldOf` + 合理默认值。对于 EventRef 用 sentinel `EventRef.NONE`。

### 2. 手动解析与 DISPATCH_CODEC 冲突 (CCE)

**崩溃信息**: `ClassCastException: RawComponent cannot be cast to Xxx`

**根因**: 手动解析创建的 RawComponent 存在类型化 key 下 → encode dispatch 到 typed CODEC → CCE。

**修复**: CODEC 失败的组件跳过（返回 null），不存 RawComponent。`parseSingleComponent` 用 try-catch 包裹 CODEC 路径。

### 3. ResourceLocation 格式冲突

**崩溃信息**: `ResourceLocationException: Non [a-z0-9/._-] character in path`

**根因**: 组件 key 使用了 ResourceLocation 不允许的格式（如双重冒号 `raw:minecraft:x`）。

**修复**: 不要用带冒号的自定义前缀。组件 key 必须符合 `namespace:path` 格式，path 只允许 `[a-z0-9/._-]`。

### 4. EventRef 字段必填

**崩溃信息**: `No key on_leash in MapLike[...]` 或 `No key tame_event in MapLike[...]`

**根因**: EventRef.CODEC 内部 `fieldOf("event")` 是合理的（事件名必填），但 EventRef 本身在被其他组件引用时用 `fieldOf("on_xxx")` 是错误的——事件字段在 Bedrock 中总是可选的。

**修复列表**: `on_leash`, `on_unleash`, `on_balloon`, `on_unballoon`, `on_give`, `on_equip`, `on_unequip`, `on_interact`, `on_look_at`, `sit_event`, `stand_event`, `tame_event` — 全部改为 `optionalFieldOf("xxx", EventRef.NONE)`。

## 降级策略

CODEC 解析失败不应导致游戏崩溃。策略：

1. `parseSingleComponent()` 的 CODEC dispatch 路径用 try-catch 包裹 → 失败跳过
2. 跳过而非存 RawComponent → 避免 encode 时 CCE
3. 后续修正 CODEC 字段后组件会自动重新识别（不用改加载器代码）
4. 对于旧世界存档：删除 `run/saves/` 避免残留序列化数据

## 验证清单

- [ ] `BehaviorEntityManager.getAllData().size()` > 0（有实体加载）
- [ ] 实体 component 类型是 typed 类（不是 EmptyComponent）
- [ ] `eyelib_debug_execute` 能正常执行（世界没崩溃）
- [ ] `saveEverything()` 不触发 CCE
- [ ] 有意义的实体（creeper/allay/player 等）component 数 > 5

## 接线验证（全量审计）

当 component_groups 或 events 桥接管道变更后（如从手动解析切换到 DISPATCH_CODEC / LogicNode.CODEC），推送全量审计确认无遗漏。

### 逐实体统计

```java
// 每实体统计：group 数、event 数、顶层 component 数、event 类型分布
java.util.List _stats = new java.util.ArrayList();
for (Object _obj : _all.values()) {
    io.github.tt432.eyelib.behavior.BehaviorEntity _be = (io.github.tt432.eyelib.behavior.BehaviorEntity) _obj;
    int _g = _be.component_groups().size();
    int _e = _be.events().size();
    int _tc = _be.components().components().size();
    if (_g > 0 || _e > 0) {
        java.util.Map _typeCount = new java.util.HashMap();
        for (Object _ev : _be.events().values()) {
            _typeCount.put(_ev.getClass().getSimpleName(),
                ((Integer) _typeCount.getOrDefault(_ev.getClass().getSimpleName(), 0)) + 1);
        }
        _stats.add(_be.identifier() + " G=" + _g + " E=" + _e + " TC=" + _tc + " ET=" + _typeCount);
    }
}
// 预期：多种 event 类型（Add/Remove/Trigger/Randomize/Sequence）出现
```

### 全量 EmptyComponent 审计

```java
// 统计三层数据中 typed vs EmptyComponent 数量
for (Object _obj : _all.values()) {
    BehaviorEntity _be = (BehaviorEntity) _obj;
    // 顶层: iterate _be.components().components().values()
    // groups: iterate _be.component_groups().values() → .components().values() → .values()
    // 检验每个 instanceof EmptyComponent
}
// 目标：顶层 100% typed，groups ≥70% typed（剩余是未实现的 behavior.* 组件）
```

### EmptyComponent key 分布确认

未命中 DISPATCH_CODEC 的组件 key 集中在：
- `minecraft:behavior.*` — 182 个 AI 行为目标尚未建模
- `minecraft:damage_sensor`, `breedable`, `angry`, `buoyant` 等 — 少数未加入 DISPATCH_CODEC 的组件

### 基准数据（2026-06-08，121 实体）

| 数据层 | typed | empty | 命中率 |
|---|---|---|---|
| 顶层 `components` | 1,707 | 0 | 100% |
| `component_groups` | 866 | 352 | 71% |
| `events` | 561 | 0 | 100% |

事件类型分布：Add(244), Remove(239), Randomize(35), Trigger(23), Sequence(20)。
