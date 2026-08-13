---
name: codec-design
description: Design and implement Codec-based data transformations. Use when creating/refactoring Codecs for serialization, making loader/schema code Codec-driven, or separating I/O from data parsing. Reference NeoForge Codec docs at https://docs.neoforged.net/docs/datastorage/codecs
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

# codec-design

设计并实现基于 Codec（DataFixerUpper）的纯数据转换：结构化解析、类型映射、验证；使 loader/schema 代码由 Codec 驱动，并将 I/O 与数据解析分离。Codec 是 DynamicOps<A> → T 的纯数据转换，不关心数据来源与去向。

## When to use
- 创建或重构用于序列化的 Codec
- 将 loader/schema 代码改为 Codec 驱动
- 需要把 I/O（文件/网络/注册表读取）与数据解析分离时
- Do NOT use when: 不涉及具体 I/O 实现与业务流程设计（属于 Codec 之外的调用方职责）

## Rules
- When 必测项:
  - 测试覆盖 partial decode：缺少字段时 DataResult.error() 包含有意义的错误信息
  - 测试覆盖边界值：空集合、null optional、极限值
- When ❌ 忽略 encode 方向:
  - NEVER 不得忽略 encode 方向、永远不要返回虚假的 encode 结果
- When ❌ 吞掉 DataResult 错误:
  - NEVER 不得吞掉 DataResult 错误（parse().result().ifPresent 丢失错误信息）；用 resultOrPartial 记录错误后再 ifPresent
- When ❌ 用 optionalFieldOf 掩盖格式错误:
  - [when 字段值可能是非法格式且希望容错时] NEVER 不得用 optionalFieldOf 掩盖格式错误（非法值会导致整个 decode 失败）；需要容错时用 lenientOptionalFieldOf 静默回退默认值
- When ❌ Codec 内做流程控制:
  - NEVER Codec 不包含业务逻辑：它是数据形状的声明；流程控制（循环、条件分支、远程调用）在调用方完成，Codec 只做数据形状转换
- When ❌ Codec 中直接访问文件/网络:
  - NEVER Codec 不接触 I/O：不读文件、不走网络、不直接调注册表，只通过 DynamicOps 操作数据；I/O 在 Codec 外部完成，传入解析好的 JsonElement/Tag
- When 铁律:
  - Codec 不接触 I/O：不读文件、不走网络、不直接调注册表，只通过 DynamicOps 操作数据；I/O 在 Codec 外部完成，传入解析好的 JsonElement/Tag
  - Codec 不包含业务逻辑：它是数据形状的声明；流程控制（循环、条件分支、远程调用）在调用方完成，Codec 只做数据形状转换
  - 每个 Codec 必须有 round-trip 测试：encode → decode 应无损恢复原始等价对象
- When 1. 组合优于继承:
  - PREFER 优先使用 xmap、comapFlatMap、withAlternative 组合已有 Codec，而非新建自定义 Codec 类
- When 2. Codec 放在类型旁边:
  - Codec 作为 public static final 字段定义在它序列化的类型中，命名为 CODEC
- When 3. 跨模块 Codec 通过 xmap 桥接:
  - [when 类型 A 在模块 M1、类型 B 在模块 M2，且两者可互相转换时] PREFER 跨模块可互转类型通过在其中一个模块用 xmap 桥接另一侧的 Codec
- When 4. 错误信息要有意义:
  - DataResult.error() 的错误信息必须包含上下文（类型名、期望值、实际值），不得是无意义的 "error"
- When 5. Partial decode 优先于完全失败:
  - PREFER Partial decode 优先于完全失败：用 setPartial/resultOrPartial 让消费者拿到部分解析结果，并在 mapError 中追加缺失信息
- When 6. 不可变集合的注意:
  - [when 需要可变集合时] listOf() 和 unboundedMap() 返回不可变集合；需要可变集合时追加 xmap 转换
- When JsonOps vs NbtOps 差异:
  - PREFER 优先在 JsonOps 下测试，再在 NbtOps 下验证（JsonOps 数字类型自动推断最窄类型，NbtOps 数字类型固定，ListTag/IntArrayTag 类型不匹配会出错）
- When RegistryOps:
  - [when Codec 需要解析注册表条目时] Codec 需要解析注册表条目时使用 RegistryOps 而非裸 JsonOps

## Workflow
1. 为类型设计 Codec 时按决策路径选择原语：固定字段 record/POJO（≤16 字段 RecordCodecBuilder，>16 拆分嵌套或自定义）；单值映射（双向无损 xmap，解码可能失败 flatXMap/comapFlatMap，范围限制 intRange/floatRange/doubleRange）；集合（listOf/unboundedMap/非 String key 用 pair-list+xmap/单值或列表 either+xmap）；多态（type 字段 dispatch，格式互斥 either/xor/withAlternative，按 key 分发 EyelibCodec.list()）；自引用（recursive，依赖未就绪 lazyInitialized）；多格式兼容（withAlternative，位置编码 TupleCodec/pair）；组合子无法表达时实现 Codec<T>/MapCodec<T> [decision]
2. 按字段语义选择 fieldOf（必填，缺失 decode 失败）/ optionalFieldOf（选填，缺失用默认值但非法值报错）/ lenientOptionalFieldOf（选填，非法值静默回退默认值） [decision]
3. 按 decode/encode 两个方向是否可能失败选择映射方法：都成功 xmap；decode 可能失败 comapFlatMap；encode 可能失败 flatComapMap；都可能失败 flatXMap [decision]
4. 组合子无法表达数据形状时直接实现 Codec<T> 或 MapCodec<T>：数据格式有特殊哨兵字段、需处理非标准顶层结构、DynamicOps 层面手动操作不可避免 [decision]

<!-- evidence background: sole source of  -->
本项目的通用 Codec 工具位于 `src/main/java/io/github/tt432/eyelib/util/codec/`：
| 工具 | 用途 |
|------|------|
| `EyelibCodec` | `list()` 分发式 MapCodec、`recursive`、`optionalMapCodec`、`withAlternative`、`int2ObjectMap`、数学/向量 Codec |
| `ChinExtraCodecs` | `singleOrList`、`check`（验证）、`treeMap`、`withAlternative(MapCodec)`、`tuple` 元组 |
| `CodecHelper` | `withAlternative`、`dispatchedMap` |
| `DispatchedMapCodec` | 按 key 分发的 Map Codec（完整实现） |
| `KeyDispatchMapCodec` | 按 key 分发的 Map Codec（每 entry 独立 dispatch） |
| `TupleCodec` | 异构元组 list 编码（1–16 个元素） |
<!-- evidence template: sole source of w-select -->
当 sealed class 的子类型通过 JSON 对象的唯一 key 区分时（如 `{"all_of": [...]}`、`{"one_of": [...]}`），用 `EyelibCodec.list()`：
```java
public static final MapCodec<ComplexFilter> CODEC = EyelibCodec.list(() -> Map.of(
    "all_of", new EyelibCodec.CodecInfo<>(AllOf.class, AllOf.CODEC),
    "one_of", new EyelibCodec.CodecInfo<>(OneOf.class, OneOf.CODEC),
    "none_of", new EyelibCodec.CodecInfo<>(NoneOf.class, NoneOf.CODEC)
));
```
<!-- evidence template: sole source of w-select -->
当数据用数组位置表达字段（如 `[1.0, 2.0, 3.0]` 表示 Vec3）时使用。
```java
public static final Codec<Vec2> CODEC = ChinExtraCodecs.tuple(Codec.FLOAT, Codec.FLOAT)
    .bmap(Vec2::new, v -> Tuple.of(v.x, v.y));
```
<!-- evidence template: sole source of w-custom -->
```java
public static final Codec<MyType> CODEC = new Codec<>() {
    @Override
    public <T> DataResult<Pair<MyType, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getMap(input).flatMap(map -> {
            // 使用 ops.get(fieldName) 手动读取字段
            // 使用子 Codec 做结构化解析
            // 返回 DataResult<Pair<MyType, T>>
        });
    }

    @Override
    public <T> DataResult<T> encode(MyType input, DynamicOps<T> ops, T prefix) {
        // 构造 RecordBuilder，build 返回 encode 结果
    }
};
```
<!-- evidence template: sole source of w-custom -->
```java
public static final MapCodec<MyType> CODEC = new MapCodec<>() {
    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.of(ops.createString("field1"), ops.createString("field2"));
    }

    @Override
    public <T> DataResult<MyType> decode(DynamicOps<T> ops, MapLike<T> input) {
        // 从 MapLike 中按 key 读取
    }

    @Override
    public <T> RecordBuilder<T> encode(MyType input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        // 往 RecordBuilder 中添加字段
    }
};
```
