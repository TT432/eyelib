---
name: codec-design
description: Design and implement Codec-based data transformations. Use when creating/refactoring Codecs for serialization, making loader/schema code Codec-driven, or separating I/O from data parsing. Reference NeoForge Codec docs at https://docs.neoforged.net/docs/datastorage/codecs
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

## 核心理念

Codec 是 **纯数据转换**（`DynamicOps<A> → T`），职责是结构化解析、类型映射、验证。它不关心数据从哪里来（文件、网络、常量），也不关心数据到哪里去。

```
数据源 (I/O 层)          Codec 管道             目标类型 (领域层)
┌──────────┐          ┌──────────────┐       ┌──────────────┐
│ JSON文本   │   parse  │ Codec<Foo>   │  →    │ Foo           │
│ NBT数据   │ ────────→│ .decode()    │  ←    │               │
│ 内存结构   │   encode │ .encode()   │       │               │
└──────────┘          └──────────────┘       └──────────────┘
```

### 铁律

- **Codec 不接触 I/O**：不读文件、不走网络、不调注册表（只通过 `DynamicOps` 操作数据）
- **Codec 不包含业务逻辑**：它是数据形状的声明，不是流程控制
- **每个 Codec 必须有 round-trip 测试**：`encode → decode` 应无损恢复原始值

---

## Codec 选型指南

当你需要为某类型设计 Codec 时，按以下决策路径选择原语：

```
类型是什么形态？
│
├─ 固定字段的 record/POJO
│   ├─ 字段 ≤ 16 → RecordCodecBuilder
│   └─ 字段 > 16 → 拆分嵌套 record 或自定义 Codec<>
│
├─ 单一值的映射/转换
│   ├─ 双向无损 → xmap
│   ├─ 解码可能失败（验证/解析） → flatXMap / comapFlatMap
│   └─ 带范围限制 → intRange / floatRange / doubleRange
│
├─ 集合类型
│   ├─ List<T> → Codec<T>.listOf()
│   ├─ Map<String, V> → Codec.unboundedMap(Codec.STRING, Codec<V>)
│   ├─ Map<K, V> (K 不是 String) → 自定义 pair-list codec + xmap
│   └─ 单值或列表兼容 → Codec.either(codec, codec.listOf()) → xmap
│
├─ 多态/变体（sealed class / tagged union）
│   ├─ 基于 type 字段分发 → Codec.dispatch(typeGetter, typeToCodec)
│   ├─ 无需 type 字段，格式互斥 → Codec.either / Codec.xor / Codec.withAlternative
│   └─ 按 key 分发（JSON 对象的 key 决定类型） → EyelibCodec.list() / MapCodec 自定义
│
├─ 自引用（递归结构）
│   ├─ 标准场景 → Codec.recursive(name, selfRef -> ...)
│   └─ 需要 lazy 初始化（依赖尚未就绪的 Codec） → Codec.lazyInitialized(supplier)
│
├─ 多格式兼容
│   ├─ 新旧格式共存，新格式优先 → Codec.withAlternative
│   ├─ 多版本按需读取（读取时选新，写入只用新） → withAlternative
│   └─ 元组/位置编码（如 [x, y, z]）→ TupleCodec / Codec.pair 链
│
└─ 无法用现有组合子表达
    └─ 实现 Codec<T> 或 MapCodec<T> 接口（见下文"自定义 Codec"）
```

---

## 核心模式

### RecordCodecBuilder — 结构化 record

最常用模式。适用条件：数据形状是固定字段的 JSON 对象 / NBT compound。

```java
public static final Codec<MyRecord> CODEC = RecordCodecBuilder.create(ins -> ins.group(
    Codec.STRING.fieldOf("name").forGetter(MyRecord::name),
    Codec.INT.optionalFieldOf("count", 0).forGetter(MyRecord::count)
).apply(ins, MyRecord::new));
```

关键决策：
- `fieldOf`：必填字段，缺失时 decode 失败
- `optionalFieldOf`：选填字段，缺失时用默认值；但遇到非法值会报错
- `lenientOptionalFieldOf`：选填字段，非法值被忽略（静默回退到默认值）

### xmap — 类型映射

当两个类型互相可无损转换时使用。

```java
// 枚举 ↔ 字符串
public static final Codec<MyEnum> CODEC = Codec.STRING.xmap(
    MyEnum::valueOf,
    MyEnum::name
);

// 外部类型适配（Vec3 ↔ List<Double>）
public static final Codec<Vec3> CODEC = Codec.DOUBLE.listOf().xmap(
    l -> new Vec3(l.get(0), l.get(1), l.get(2)),
    v -> List.of(v.x, v.y, v.z)
);
```

### flatXMap — 带验证的类型映射

当转换可能失败时需要 `DataResult`。

```java
public static final Codec<Integer> POSITIVE_INT = Codec.INT.flatXMap(
    i -> i > 0
        ? DataResult.success(i)
        : DataResult.error(() -> "Expected positive, got " + i),
    DataResult::success  // encode 方向不会失败
);
```

方向矩阵：

| decode 方向 | encode 方向 | 方法 |
|-----------|-----------|------|
| 总成功 | 总成功 | `xmap` |
| 可能失败 | 总成功 | `comapFlatMap` |
| 总成功 | 可能失败 | `flatComapMap` |
| 可能失败 | 可能失败 | `flatXMap` |

### Dispatch — 多态分发

当数据包含 `type` 字段决定子类型时使用。

```java
// 定义 type 获取器和 codec 查找表
public static final Codec<Animal> CODEC = Codec.STRING.dispatch(
    Animal::type,        // type 字段值
    type -> switch (type) {
        case "cat" -> Cat.CODEC;
        case "dog" -> Dog.CODEC;
        default -> throw new IllegalStateException("Unknown type: " + type);
    }
);
```

如果需要按 key 分发（JSON 的顶级 key 决定类型），参考下一节。

### Tagged Union (Key Dispatch)

当 sealed class 的子类型通过 JSON 对象的唯一 key 区分时（如 `{"all_of": [...]}`、`{"one_of": [...]}`），用 `EyelibCodec.list()`：

```java
public static final MapCodec<ComplexFilter> CODEC = EyelibCodec.list(() -> Map.of(
    "all_of", new EyelibCodec.CodecInfo<>(AllOf.class, AllOf.CODEC),
    "one_of", new EyelibCodec.CodecInfo<>(OneOf.class, OneOf.CODEC),
    "none_of", new EyelibCodec.CodecInfo<>(NoneOf.class, NoneOf.CODEC)
));
```

特点：每个变体通过 `MapCodec.fieldOf(key)` 自动包装，encode 时写回对应的 key。

### withAlternative — 多格式兼容

读取时接受多种格式，写入时只用主格式。

```java
// 新格式 [x, y, z] 或旧格式 {"x": 1, "y": 2, "z": 3}
public static final Codec<BlockPos> CODEC = Codec.withAlternative(
    BlockPos.CODEC,           // 主格式（用于 encode）
    LEGACY_POS_CODEC          // 兼容格式（只用于 decode）
);
```

### 单值或列表兼容

```java
// 接受 "foo" 或 ["foo", "bar"]
public static Codec<List<String>> singleOrList(Codec<String> codec) {
    return Codec.either(codec, codec.listOf())
        .xmap(e -> e.map(List::of, Function.identity()), Either::right);
}
```

### 自定义 MapCodec — 动态字段

当 record 的字段名不是编译期常量，而需要从数据中动态读取时，实现自定义 `MapCodec`。

```java
// Map<K, V> 的 codec，其中 value 的 codec 由 key 决定
public record DispatchedMapCodec<K, V>(
    Codec<K> keyCodec,
    Function<K, Codec<? extends V>> valueCodecFunction
) implements Codec<Map<K, V>> {
    // decode: 遍历 map 的每个 entry，用 keyCodec 解析 key，
    //         用 key 查找对应的 valueCodec，解析 value
    // encode: 遍历 map，分别 encode key 和 value
}
```

常见变体：
- **KeyDispatchMapCodec**：和 DispatchedMapCodec 类似但 value 的 Codec 通过 key 查找，不通过 key Codec
- **自定义顶层 Codec<>**：当 JSON 结构有特殊规则（如某字段与数据平级），直接实现 `Codec<T>` 接口

### Recursive — 自引用类型

```java
public static final Codec<TreeNode> CODEC = Codec.recursive(
    "TreeNode",
    self -> RecordCodecBuilder.create(ins -> ins.group(
        Codec.STRING.fieldOf("value").forGetter(TreeNode::value),
        self.listOf().optionalFieldOf("children", List.of()).forGetter(TreeNode::children)
    ).apply(ins, TreeNode::new))
);
```

### TupleCodec — 位置编码

当数据用数组位置表达字段（如 `[1.0, 2.0, 3.0]` 表示 Vec3）时使用。

```java
public static final Codec<Vec2> CODEC = ChinExtraCodecs.tuple(Codec.FLOAT, Codec.FLOAT)
    .bmap(Vec2::new, v -> Tuple.of(v.x, v.y));
```

---

## 自定义 Codec 实现

当 `RecordCodecBuilder`、`xmap`、`dispatch` 等组合子无法表达数据形状时，直接实现 `Codec<T>` 或 `MapCodec<T>`。

### 何时需要自定义

- 数据格式有"特殊哨兵字段"（如 `version` 字段和数据条目在同一层级）
- 需要处理非标准（非 JSON 对象）的顶层结构
- `DynamicOps` 层面的手动操作不可避免

### Codec<T> 模板

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

### MapCodec<T> 模板

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

---

## 测试 Codec

### 必测项

1. **Round-trip**：`encode → decode` 得到等价对象
2. **Partial decode**：缺少字段时 `DataResult.error()` 包含有意义的错误信息
3. **边界值**：空集合、null optional、极限值

### 模板

```java
class MyCodecTest {
    static final Codec<MyType> CODEC = MyType.CODEC;

    @Test
    void roundTrip() {
        var original = new MyType("test", 42);
        var encoded = CODEC.encodeStart(JsonOps.INSTANCE, original)
            .getOrThrow(false, msg -> fail(msg));
        var decoded = CODEC.parse(JsonOps.INSTANCE, encoded)
            .getOrThrow(false, msg -> fail(msg));
        assertEquals(original, decoded);
    }

    @Test
    void missingRequiredField() {
        var json = new JsonObject(); // 缺少必填字段
        var result = CODEC.parse(JsonOps.INSTANCE, json);
        assertTrue(result.error().isPresent());
    }

    @Test
    void jsonStructureSnapshot() {
        // snapshot 测试：确保 encode 输出结构稳定
        var obj = new MyType("test", 42);
        var json = CODEC.encodeStart(JsonOps.INSTANCE, obj)
            .getOrThrow(false, AssertionError::new);
        assertEquals("""
            {"name":"test","count":42}""", json.toString());
    }
}
```

---

## Codec 设计原则

### 1. 组合优于继承

优先使用 `xmap`、`comapFlatMap`、`withAlternative` 组合已有 Codec，而非新建类。

```java
// ✅ 组合
public static final Codec<MyType> CODEC = Codec.STRING.xmap(MyType::parse, MyType::toString);

// ❌ 过度自定义（除非必要）
public static final Codec<MyType> CODEC = new Codec<>() { /* 50 行实现 */ };
```

### 2. Codec 放在类型旁边

Codec 作为 `public static final` 字段定义在它序列化的类型中，命名为 `CODEC`。

```java
public record Foo(String name, int count) {
    public static final Codec<Foo> CODEC = RecordCodecBuilder.create(/* ... */);
}
```

### 3. 跨模块 Codec 通过 xmap 桥接

当类型 A 在模块 M1，类型 B 在模块 M2，且两者可互相转换时：

```java
// 在 M2 中
public static final Codec<B> CODEC = A.CODEC.xmap(A::toB, B::toA);
```

### 4. 错误信息要有意义

```java
// ❌ 无意义的错误
DataResult.error(() -> "error")

// ✅ 包含上下文
DataResult.error(() -> "Failed to decode MyType: expected positive int, got " + value)
```

### 5. Partial decode 优先于完全失败

使用 `setPartial` 或 `resultOrPartial` 让消费者能拿到部分解析结果：

```java
return result.map(unit -> elements)
    .setPartial(elements)   // 即使有错误，也返回已解析的部分
    .mapError(e -> e + " missed input: " + errors);
```

### 6. 不可变集合的注意

`listOf()` 和 `unboundedMap()` 返回不可变集合。如果需要可变集合，追加 `xmap`：

```java
public static final Codec<List<String>> CODEC = Codec.STRING.listOf()
    .xmap(ArrayList::new, Function.identity());
```

---

## 常见反模式

### ❌ Codec 中直接访问文件/网络

```java
// 错误
Codec<Config> BAD = RecordCodecBuilder.create(ins -> ins.group(
    Codec.STRING.fieldOf("data").forGetter(c ->
        Files.readString(Path.of("config.json")) // NEVER
    )
).apply(ins, Config::new));
```

**正确**：在 Codec 外部完成 I/O，传入解析好的 `JsonElement` / `Tag`。

### ❌ 吞掉 DataResult 错误

```java
// 错误：丢失错误信息
codec.parse(ops, input).result().ifPresent(map::put);
```

**正确**：
```java
codec.parse(ops, input).resultOrPartial(err -> log.warn("Decode error: {}", err))
    .ifPresent(map::put);
```

### ❌ Codec 内做流程控制

```java
// 错误：Codec 内做循环、条件分支等业务逻辑
Codec<List<Foo>> BAD = Codec.STRING.listOf().comapFlatMap(names -> {
    for (String name : names) {
        fetchRemote(name); // NEVER
    }
    return DataResult.success(List.of());
});
```

**正确**：流程控制在调用方，Codec 只做数据形状转换。

### ❌ 用 optionalFieldOf 掩盖格式错误

```java
// 情况：字段值是非法格式时，optionalFieldOf 会报错终止解析
// 如果希望容错，用 lenientOptionalFieldOf

// ❌ 非法值会导致整个 decode 失败
Codec.INT.optionalFieldOf("count", 0).forGetter(Foo::count)

// ✅ 非法值被静默忽略，使用默认值
Codec.INT.lenientOptionalFieldOf("count", 0).forGetter(Foo::count)
```

### ❌ 忽略 encode 方向

如果只需要 decode（只读配置），至少把 encode 方向实现为抛异常并加注释说明：

```java
// 编码不支持（此 Codec 仅用于解码）
Codec.BYTE_BUFFER.comapFlatMap(
    bytes -> /* decode logic */,
    obj -> { throw new UnsupportedOperationException("encode not supported"); }
);
```

永远不要返回虚假的 encode 结果。

---

## NBT 专用考量

### JsonOps vs NbtOps 差异

```java
// JsonOps: 数字类型自动推断最窄类型
// NbtOps: 数字类型固定（Tag 有明确的类型标记）

// 危险：JsonOps 下工作正常，NbtOps 下 ListTag 类型不匹配会导致错误
Codec.INT.listOf()  // NbtOps 下只能从 IntArrayTag 解码，不能从 ListTag 解码
```

解决方案：优先在 `JsonOps` 下测试，再在 `NbtOps` 下验证。

### RegistryOps

当 Codec 需要解析注册表条目时，使用 `RegistryOps` 而非裸 `JsonOps`：

```java
RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, lookupProvider);
codec.parse(ops, json);
```

---

## 项目内 Codec 工具引用

本项目的通用 Codec 工具位于 `src/main/java/io/github/tt432/eyelib/util/codec/`：

| 工具 | 用途 |
|------|------|
| `EyelibCodec` | `list()` 分发式 MapCodec、`recursive`、`optionalMapCodec`、`withAlternative`、`int2ObjectMap`、数学/向量 Codec |
| `ChinExtraCodecs` | `singleOrList`、`check`（验证）、`treeMap`、`withAlternative(MapCodec)`、`tuple` 元组 |
| `CodecHelper` | `withAlternative`、`dispatchedMap` |
| `DispatchedMapCodec` | 按 key 分发的 Map Codec（完整实现） |
| `KeyDispatchMapCodec` | 按 key 分发的 Map Codec（每 entry 独立 dispatch） |
| `TupleCodec` | 异构元组 list 编码（1–16 个元素） |

---

## 参考资料

- [NeoForge Codec 文档](https://docs.neoforged.net/docs/datastorage/codecs)
- [Mojang DataFixerUpper 源码](https://github.com/Mojang/DataFixerUpper) — `Codec`、`DataResult`、`DynamicOps` 的定义
- `com.mojang.serialization.Codec` — 标准库 Codec 组合子的完整列表
- `net.minecraft.util.ExtraCodecs` — Minecraft 提供的额外 Codec
