# Behavior Entity 事件节点系统：现状分析与最小可行改造方案

**日期:** 2026-06-02
**范围:** event 节点类型 gap 分析（非 components 链路）
**前置文档:** `BEHAVIOR_ENTITY_REFACTOR_PLAN.md`（components 改造）

---

## 1. 当前实现状态

### 1.1 文件结构

```
eyelib-behavior/src/main/java/.../eyelibbehavior/event/
├── EntityEvent.java              # 空标记类（占位）
├── EntitySpawned.java            # 空标记类（占位）
├── logic/
│   ├── LogicNode.java            # LogicNode 接口 + CODEC 调度
│   ├── Add.java                  # add 节点
│   ├── Remove.java               # remove 节点
│   ├── Sequence.java             # sequence 节点
│   └── Randomize.java            # randomize 节点
├── filter/
│   ├── Filter.java               # Filter 接口
│   ├── ComplexFilter.java        # all_of / one_of / none_of
│   ├── Subject.java              # 主体枚举
│   ├── Operator.java             # 操作符枚举
│   └── base/
│       ├── BaseFilter.java       # 过滤器抽象基类（sealed）
│       └── ActorHealth.java      # actor_health 过滤器（唯一实现）
├── filter/package-info.java
├── logic/package-info.java
└── package-info.java
```

### 1.2 已实现的事件节点类型（4 种）

| 节点 | 文件 | 字段 | Codec | 执行逻辑 |
|------|------|------|-------|---------|
| `add` | `Add.java` | `List<String> component_groups` | RecordCodecBuilder | 从 behavior 的 component_groups map 中查找并添加到 data 列表 |
| `remove` | `Remove.java` | `List<String> component_groups` | RecordCodecBuilder | 从 data 的 component_groups 列表中移除匹配项 |
| `sequence` | `Sequence.java` | `List<LogicNode> nodes` | LogicNode.CODEC.listOf() | 依次执行所有子节点（无过滤） |
| `randomize` | `Randomize.java` | `List<Entry> entries` (weight + LogicNode) | Entry.CODEC 列表 | 加权轮盘赌（二分查找）选择一个 Entry 执行 |

### 1.3 LogicNode 调度机制

```java
// LogicNode.java — 类型分发的核心
MapCodec<LogicNode> CODEC = list(() -> Map.of(
    "add",       new CodecInfo<>(Add.class, Add.CODEC),
    "randomize", new CodecInfo<>(Randomize.class, Randomize.CODEC),
    "sequence",  new CodecInfo<>(Sequence.class, Sequence.CODEC),
    "remove",    new CodecInfo<>(Remove.class, Remove.CODEC)
));
```

使用 `EyelibCodec.list()` 创建 `MapCodec`，通过 JSON 字段名匹配到具体类型的 codec。调用 `CODEC.codec()` 得到完整的 `Codec<LogicNode>`，用于 `BehaviorEntity.events` map。

### 1.4 事件数据流

```
behavior JSON "events" 区段
    → BehaviorEntity.CODEC 解析
    → BehaviorEntity(..., events: Map<String, LogicNode>)
    → EntityBehaviorData (持有 behavior 引用)
    → 外部调用 data.getBehavior().events().get("event_name").eval(data)
```

---

## 2. Bedrock 标准事件节点类型

### 2.1 完整节点清单（~15 种，含复合变体）

| # | 节点名称 | Bedrock JSON 示例 | 用途 | 当前状态 |
|---|---------|-------------------|------|---------|
| 1 | `add` | `"add": { "component_groups": [...] }` | 添加组件组 | ✅ 已实现 |
| 2 | `remove` | `"remove": { "component_groups": [...] }` | 移除组件组 | ✅ 已实现 |
| 3 | `sequence` | `"sequence": [{ "filters":..., "add":... }, ...]` | 条件顺序执行 | ⚠️ 有壳无瓤 |
| 4 | `randomize` | `"randomize": [{ "weight": 95, "add":... }, ...]` | 加权随机选择 | ✅ 已实现 |
| 5 | `trigger` | `"trigger": { "event": "spawn_baby", "target": "self" }` | 触发另一事件 | ❌ 缺失 |
| 6 | `queue_command` | `"queue_command": { "command": "summon pig", "target": "self" }` | 执行/队列命令 | ❌ 缺失 |
| 7 | `set_property` | `"set_property": { "wiki:flag": false }` | 设置实体属性 | ❌ 缺失 |
| 8 | `filters`(在 sequence 内) | `"filters": { "test": "has_component", ... }` | 条件门控 | ⚠️ 已有 Filter 接口但未接入 LogicNode |
| 9 | `run_command` | (旧版, 已被 `queue_command` 取代) | 执行命令 | ❌ 废弃 |
| 10-13 | _复合变体_: `sequence` 内嵌套 `randomize`, `sequence` 内嵌套 `trigger`, `randomize` 内嵌套 `sequence`, `randomize` 内嵌套 `trigger` 等 | 见下文 | 组合使用 | ⚠️ 取决于基础节点 |

### 2.2 关键 Bedrock 模式分析

#### 模式 A: `trigger` 作为顶级节点（最常用）

piglin.json 中反复使用的模式：
```json
"minecraft:entity_born": {
    "trigger": "spawn_baby"
}
```
以及带 target 的完整形式：
```json
"trigger": {
    "event": "wiki:interacted",
    "target": "other",
    "filters": { "test": "is_family", "subject": "self", "value": "pig" }
}
```

**Bedrock 特殊规则:** `trigger` 的 value 可以是字符串（简化触发同一实体的另一事件），也可以是完整对象（带 target 和 filters）。关键：**target 机制允许跨实体触发事件**（self / other / parent / target / baby 等）。

#### 模式 B: `sequence` 带 `filters` 门控

zombie_villager_v2.json 中的核心模式：
```json
"minecraft:entity_spawned": {
    "sequence": [
        {
            "filters": { "test": "has_component", "operator": "!=", "value": "minecraft:variant" },
            "randomize": [
                { "weight": 9500, "add": { "component_groups": ["adult"] } },
                { "weight": 75,   "add": { "component_groups": ["baby", "jockey"] } }
            ]
        },
        {
            "filters": { ... },
            "randomize": [ ... ]
        }
    ]
}
```

**当前 `Sequence` 的问题:** 直接执行 `List<LogicNode>`，每个子节点只是一个 LogicNode，缺少 **filters 门控层**。在 Bedrock 中，sequence 的每个条目是一个"带可选 filters 的 logic 容器"，而不是裸 LogicNode。

#### 模式 C: `randomize` 与 `trigger`/`add` 混合

```json
"minecraft:entity_spawned": {
    "randomize": [
        { "weight": 5,  "trigger": "spawn_baby" },
        { "weight": 95, "trigger": "spawn_adult" }
    ]
}
```

**关键:** `randomize` 的每个 entry 不仅支持 `add`/`remove`，还直接支持 `trigger` 作为动作。当前实现中 `Entry` 只持有一个 `LogicNode`，但 `LogicNode` 的注册表可以扩展。

---

## 3. GAP 分析

### 3.1 功能差距矩阵

| 方面 | 当前状态 | Bedrock 标准 | 严重度 | 备注 |
|------|---------|-------------|--------|------|
| 节点类型覆盖 | 4/4 个基础 | ~15 种 | **高** | 6 种关键节点缺失 |
| `trigger` 支持 | ❌ 不存在 | 最常用节点之一 | **P0** | 无法事件链式调用 |
| `queue_command` | ❌ 不存在 | gameplay 核心 | **P0** | 无法执行命令 |
| `set_property` | ❌ 不存在 | 1.20+ 新特性 | **P1** | 无法控制实体属性 |
| `sequence` filters 门控 | ❌ 无 filter 集成 | 每个 sequence 条目都可选 filters | **P0** | 条件执行全靠 filters |
| 递归嵌套 | ⚠️ 基础支持 | `randomize` 内嵌 `sequence` 等 | **P1** | 架构支持但节点集不足 |
| `filters` 作为 LogicNode | ❌ 不存在 | 可在 sequence 中用作门控 | **P0** | 有 Filter 体系但未接入 LogicNode |
| 事件 target（跨实体） | ❌ 不存在 | self/other/parent/target/baby | **P2** | 需要实体上下文传递 |
| Molang 表达式 | ❌ 不存在 | `set_property` 需要 | **P2** | 需要 Molang 引擎 |
| 空事件兼容 | ⚠️ 可能报错 | `"remove": {}` 合法 | **P3** | 空 `remove`/`add` 合法 |

### 3.2 架构差距分析

```
当前 LogicNode 树结构:
  LogicNode (interface)
  ├── Add(List<String> component_groups)
  ├── Remove(List<String> component_groups)
  ├── Sequence(List<LogicNode> nodes)          ← 无 filter 层
  └── Randomize(List<Entry>)
       └── Entry(int weight, LogicNode node)

Bedrock 实际语义树:
  EventResponseNode (任何一个都可以是顶级节点或嵌套节点)
  ├── Add(List<String> component_groups)       ← 可选 [filters, target]
  ├── Remove(List<String> component_groups)    ← 可选 [filters, target]
  ├── Trigger(String event, Target target, Filters filters)
  ├── Sequence(List<SequenceEntry>)
  │    └── SequenceEntry                       ← 关键中间层!
  │         ├── filters? Filter                ← 条件门控
  │         └── response EventResponseNode     ← 任意子节点
  ├── Randomize(List<RandomEntry>)
  │    └── RandomEntry
  │         ├── weight int
  │         └── response EventResponseNode     ← 任意子节点
  ├── QueueCommand(String|List<String> command, Target target)
  └── SetProperty(Map<String, String|Boolean|Int>)
```

**最关键的架构缺失：** 缺少 `SequenceEntry` / `RandomEntry` 内部可以承载 `filters + 任意子节点` 的中间层。当前 `Sequence` 直接取 `List<LogicNode>`，没有为每个条目附加 filters 的能力。

---

## 4. 最小可行改造方案

### 4.1 优先级推荐

| 优先级 | 节点 | 理由 | 预估工作量 |
|--------|------|------|-----------|
| **P0** | `filters` 接入 LogicNode | sequence 条件执行的基础 | 2 个文件，~40 行 |
| **P0** | `trigger` | 事件链式调用的核心，piglin 等实体大量使用 | 3 个文件，~80 行 |
| **P0** | `queue_command` | 实现自定义交互逻辑的必要条件 | 2 个文件，~60 行 |
| **P1** | `set_property` | 1.20+ 新特性，控制实体状态 | 2 个文件，~50 行 |
| **P2** | target 跨实体机制 | 需要实体引用传递，涉及更广 | 多个文件，~120 行 |

### 4.2 实施顺序

```
Phase 1 — 基础架构扩展（P0）
├── 1.1 新增 SequenceEntry 中间层 → 使 sequence 支持 filters
├── 1.2 新增 Trigger 节点
├── 1.3 新增 QueueCommand 节点
└── 1.4 注册新节点到 LogicNode.CODEC

Phase 2 — 完整功能（P1）
├── 2.1 新增 SetProperty 节点
├── 2.2 实现后续 filter（has_component, is_family, is_underwater 等）
└── 2.3 Randomize 条目支持 filter 门控

Phase 3 — 高级特性（P2）
├── 3.1 Target 跨实体触发
├── 3.2 RunCommand 废弃兼容（转为 QueueCommand）
└── 3.3 空事件处理（remove: {} / add: {}）
```

### 4.3 Phase 1 详细设计

#### 1.3.1 新增 SequenceEntry — sequence 带 filters 门控

**设计理由:** Bedrock 的 `sequence` 中每个条目都**可选**带 `filters`，当前 `Sequence` 的 `List<LogicNode>` 模型丢失了这个语义。

**改造路径：方案 A（推荐）— 最小侵入**
不改 `Sequence` 类名/构造，新增 `SequenceEntry` record，让 `Sequence` 持有 `List<SequenceEntry>` 替代 `List<LogicNode>`。

```java
// 新增 SequenceEntry.java (package io.github.tt432.eyelibbehavior.event.logic)
public record SequenceEntry(
        @Nullable Filter filter,
        LogicNode node
) implements LogicNode {
    public static final Codec<SequenceEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Filter.CODEC.optionalFieldOf("filters").forGetter(e -> Optional.ofNullable(e.filter)),
            LogicNode.CODEC.codec().forGetter(e -> e.node)
    ).apply(ins, (f, n) -> new SequenceEntry(f.orElse(null), n)));

    @Override
    public void eval(EntityBehaviorData data) {
        if (filter == null || filter.eval(data)) {
            node.eval(data);
        }
    }
}
```

**注意:** 此处 `filter.eval(data)` 需要 Filter 接口增加执行方法。当前 Filter 只是编解码接口，尚无执行逻辑。这引出一个依赖项：**需要为 Filter 实现 `eval(EntityBehaviorData)` 方法**。

```java
// Filter.java — 增加执行方法
public interface Filter {
    Codec<Filter> CODEC = ...; // 现有编码不变
    boolean eval(EntityBehaviorData data); // 新增
}
```

然后改造 `Sequence.java`:

```java
// 改造后的 Sequence.java
public record Sequence(
        List<SequenceEntry> entries
) implements LogicNode {
    public static final Codec<Sequence> CODEC = SequenceEntry.CODEC.listOf()
            .xmap(Sequence::new, Sequence::entries);

    @Override
    public void eval(EntityBehaviorData data) {
        for (SequenceEntry entry : entries) {
            entry.eval(data);
        }
    }
}
```

**向后兼容:** 原有的 `sequence: [LogicNode, ...]` JSON 数据无法直接兼容。需要处理裸 LogicNode（无 filters 字段）也能被解析。可以在 `SequenceEntry.CODEC` 中增加一个 fallback 逻辑：如果 JSON 对象中既没有 `filters` 也没有可识别的节点字段名，则将整个对象作为 LogicNode 解析。

> 简化方案：Phase 1 暂时要求 sequence entry 必须显式包裹，先支持 `sequence: [{ "add": {...} }]` 形式。裸 `sequence: [ { ...logic node fields... } ]` 本身在 Bedrock 中就是标准形式——每个 entry 是一个对象，内部包含 filters/add/remove/trigger 等字段。不需要额外兼容旧格式。

#### 1.3.2 新增 Trigger 节点

```java
// Trigger.java
public record Trigger(
        @Nullable Filter filter,
        String event,
        Subject target  // 默认 self
) implements LogicNode {
    public static final Codec<Trigger> CODEC = Codec.either(
            // 简化形式: "trigger": "event_name"
            Codec.STRING,
            // 完整形式
            RecordCodecBuilder.<Trigger>create(ins -> ins.group(
                    Filter.CODEC.optionalFieldOf("filters").forGetter(t -> Optional.ofNullable(t.filter)),
                    Codec.STRING.fieldOf("event").forGetter(Trigger::event),
                    Subject.CODEC.optionalFieldOf("target", Subject.self).forGetter(Trigger::target)
            ).apply(ins, (f, e, t) -> new Trigger(f.orElse(null), e, t)))
    ).xmap(
            either -> either.map(s -> new Trigger(null, s, Subject.self), Function.identity()),
            t -> t.filter == null && t.target == Subject.self ? Either.left(t.event) : Either.right(t)
    );

    @Override
    public void eval(EntityBehaviorData data) {
        if (filter != null && !filter.eval(data)) return;
        // 根据 target 查找目标实体，执行其事件
        // 简化: 先实现 self 情况
        data.getBehavior().ifPresent(b -> {
            LogicNode targetEvent = b.events().get(event);
            if (targetEvent != null) {
                targetEvent.eval(data);
            }
        });
    }
}
```

**Bedrock 规范:**
- 简化形式: `"trigger": "event_name"` → 等价于 `{ "event": "event_name", "target": "self" }`
- 完整形式: `"trigger": { "event": "...", "target": "...", "filters": {...} }`
- target 可选值: self, other, parent, target, baby, player, damager, block

**Phase 1 范围:** 仅实现 self target。跨实体 target 留到 Phase 3。

#### 1.3.3 新增 QueueCommand 节点

```java
// QueueCommand.java
public record QueueCommand(
        @Nullable Subject target,
        List<String> command  // 单字符串或字符串数组
) implements LogicNode {
    public static final Codec<QueueCommand> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Subject.CODEC.optionalFieldOf("target", Subject.self).forGetter(QueueCommand::target),
            Codec.either(Codec.STRING, Codec.STRING.listOf())
                    .xmap(
                            e -> e.map(List::of, Function.identity()),
                            l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
                    ).fieldOf("command").forGetter(QueueCommand::command)
    ).apply(ins, QueueCommand::new));

    @Override
    public void eval(EntityBehaviorData data) {
        // 将命令入队，在 tick 结束时执行
        // 需要对接 Minecraft 命令系统
        // Phase 1 实现: 通过日志输出命令，后续对接实际命令执行器
        for (String cmd : command) {
            // CommandQueue.enqueue(target, cmd);
        }
    }
}
```

**Bedrock 规范:**
- `"queue_command": { "target": "self", "command": "summon pig" }`
- `"queue_command": { "target": "self", "command": ["summon pig", "say hi"] }`
- target 可选，默认 self

**Phase 1 范围:** 实现编解码和命令队列存储，命令执行器接口占位。

#### 1.3.4 更新 LogicNode.CODEC 注册表

```java
// LogicNode.java — 更新后的注册表
MapCodec<LogicNode> CODEC = list(() -> Map.of(
    "add",          new CodecInfo<>(Add.class, Add.CODEC),
    "remove",       new CodecInfo<>(Remove.class, Remove.CODEC),
    "sequence",     new CodecInfo<>(Sequence.class, Sequence.CODEC),
    "randomize",    new CodecInfo<>(Randomize.class, Randomize.CODEC),
    "trigger",      new CodecInfo<>(Trigger.class, Trigger.CODEC),
    "queue_command", new CodecInfo<>(QueueCommand.class, QueueCommand.CODEC)
));
```

### 4.4 Phase 2 详细设计

#### 1.4.1 新增 SetProperty 节点

```java
// SetProperty.java
public record SetProperty(
        Map<String, String> properties  // value 可以是 string/bool/int，统一用 String + Molang
) implements LogicNode {
    public static final Codec<SetProperty> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
            .fieldOf("set_property")
            .xmap(SetProperty::new, SetProperty::properties);

    @Override
    public void eval(EntityBehaviorData data) {
        // 需要实体属性系统支持
        // Phase 2 实现: 仅记录 property 变更，实际生效依赖 Molang 引擎
    }
}
```

**Bedrock 规范:**
```json
"set_property": {
    "wiki:boolean_property_example": false,
    "wiki:integer_property_example": "q.property('wiki:integer_property_example') + 1"
}
```

**依赖项:** 实体属性系统 + Molang 表达式引擎。如果两者都未就绪，`SetProperty` 可以先行实现 codec 和数据结构，执行逻辑用 stubs。

#### 1.4.2 扩展 Randomize Entry 支持嵌套 Sequence/Filter

当前 Randomize.Entry 已经通过 LogicNode 支持任意子节点嵌套（包括 sequence/randomize/trigger），架构上不需要改造——**只需将新增节点注册到 LogicNode.CODEC**，Randomize 自然获得嵌套能力。

### 4.5 文件修改清单

| # | 操作 | 文件 | 复杂度 |
|---|------|------|--------|
| **Phase 1** | | | |
| 1 | 新增 | `event/logic/SequenceEntry.java` | 简单 (~40 行) |
| 2 | 修改 | `event/logic/Sequence.java` — 用 SequenceEntry 替代 LogicNode | 简单 (~5 行) |
| 3 | 修改 | `event/filter/Filter.java` — 增加 `eval(data)` 方法 | 简单 (~2 行) |
| 4 | 修改 | `event/filter/base/BaseFilter.java` — 实现 eval | 中等 (~20 行) |
| 5 | 修改 | `event/filter/ComplexFilter.java` — 实现 eval | 中等 (~30 行) |
| 6 | 新增 | `event/logic/Trigger.java` | 中等 (~80 行) |
| 7 | 新增 | `event/logic/QueueCommand.java` | 中等 (~60 行) |
| 8 | 修改 | `event/logic/LogicNode.java` — 注册新节点 | 简单 (~3 行) |
| **Phase 2** | | | |
| 9 | 新增 | `event/logic/SetProperty.java` | 中等 (~50 行) |
| 10 | 修改 | `event/logic/LogicNode.java` — 注册 set_property | 简单 (~1 行) |
| 11 | 新增 | `event/logic/CommandQueue.java` — 命令队列服务 | 中等 (~50 行) |
| **Phase 3** | | | |
| 12 | 修改 | `Trigger.java` — 跨实体 target 支持 | 复杂 (~80 行) |
| 13 | 新增 | 多个 Filter 实现（has_component, is_family, etc.） | 每个 ~20 行 |

---

## 5. 测试策略

### 5.1 Unit Tests

```java
// SequenceEntry 测试
void sequenceEntryWithoutFilterAlwaysExecutes() {
    var entry = new SequenceEntry(null, addNode);
    entry.eval(data);
    // verify add was applied
}

void sequenceEntryWithFilterBlocksExecution() {
    var filter = mock(Filter.class);
    when(filter.eval(any())).thenReturn(false);
    var entry = new SequenceEntry(filter, addNode);
    entry.eval(data);
    // verify add was NOT applied
}

// Trigger 测试
void triggerCallsEventByName() {
    var trigger = new Trigger(null, "test_event", Subject.self);
    trigger.eval(data);
    // verify "test_event" was evaluated
}

void triggerStringShortFormWorks() {
    // 验证 Codec.either 能解析 "trigger": "event_name"
    var parsed = Trigger.CODEC.parse(...);
    assertEquals("event_name", parsed.event());
    assertEquals(Subject.self, parsed.target());
}

// QueueCommand 测试
void queueCommandParsesSingleString() {
    // "command": "summon pig"
    var parsed = QueueCommand.CODEC.parse(...);
    assertEquals(List.of("summon pig"), parsed.command());
}

void queueCommandParsesStringArray() {
    // "command": ["summon pig", "say hi"]
    var parsed = QueueCommand.CODEC.parse(...);
    assertEquals(2, parsed.command().size());
}
```

### 5.2 集成测试

使用 vanilla behavior pack 中真实实体的 events 区段 JSON 进行 round-trip 解析：
- piglin.json 的 `events` 段（含 trigger + randomize 嵌套）
- zombie_villager_v2.json 的 events 段（含 sequence + filters + randomize 嵌套）
- cow.json 的 events 段（含 randomize + trigger 嵌套）

---

## 6. 风险与依赖

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Filter.eval() 需要 EntityBehaviorData 访问完整实体状态 | 无法正确执行 filter 逻辑 | Phase 1 先实现基础的 has_component 和恒真/恒假 filter |
| Sequence 改造后旧 JSON 格式可能不兼容 | 已有测试数据可能解析失败 | 在 SequenceEntry.CODEC 中增加向下兼容适配层 |
| Trigger 跨实体 target 需要实体查找能力 | self 以外 target 需 Phase 3 | Phase 1 仅实现 self target |
| QueueCommand 命令执行需要对接 Minecraft 命令系统 | 无法实际执行命令 | Phase 1 仅实现编解码和存储，执行器用接口占位 |
| SetProperty 依赖实体属性系统 | 无属性系统则无法评估 | 先行实现 codec，执行 stub，等属性系统就绪后再激活 |

---

## 7. 总结

### 当前 gap
- 已实现: `add`, `remove`, `sequence`（无 filter 门控）, `randomize` — 4 种基础节点
- 缺失关键节点: `trigger`（P0）, `queue_command`（P0）, `set_property`（P1）, 以及 `sequence` 的 filter 门控能力（P0）
- 架构缺失: Sequence 缺少 SequenceEntry 中间层；Filter 缺少 eval 执行方法

### Phase 1 产出（最小可行）
新增 3 个节点（SequenceEntry、Trigger、QueueCommand）+ 扩展 Filter 接口，使事件系统能覆盖 Bedrock 日常使用场景（piglin、zombie_villager 等标准实体的 events）。

### 核心原则
1. **不改 importer 层** — 所有改动集中在 runtime 的事件解析和执行层
2. **codec 先行** — 每次先确保解析/序列化正确，执行逻辑可以迭代
3. **向后兼容** — 新增字段都是 optionalFieldOf，旧 JSON 不破坏
4. **最少依赖** — Phase 1 不依赖 Molang 引擎、实体属性系统、跨实体查找
