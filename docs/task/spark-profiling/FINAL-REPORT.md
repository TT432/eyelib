# Eyelib 性能基准测试与优化建议报告

> 基于 spark profiler 实测数据。所有结论有 spark raw protobuf / heapsummary JSON 支撑，无猜测。

## 任务概述

使用 spark profiler 对 eyelib dev 客户端进行三类基准测试，识别性能瓶颈并给出优化建议。

| 测试 | 场景 | profile | 数据文件 |
|---|---|---|---|
| T2 | 稳态渲染（59 eyelib slime，60s） | https://spark.lucko.me/YsDkEySi5t | `data/t2-render-pb-sparkc.bin`（2.8MB） |
| T3 | 资源重载（F3+T，90s） | https://spark.lucko.me/EA6AAqdMUF | `data/t3-reload-pb.bin`（3.07MB） |
| T4 | 堆内存摘要 | https://spark.lucko.me/8RhM134XVh | `data/t4-heapsummary-full.json`（4.15MB） |

**测试负载**：Actions-and-Stuff mcpack（113 实体定义）+ 59 个 eyelib 接管渲染的 slime。

---

## 优化建议总表（按预期收益排序）

| 优先级 | 优化项 | 场景 | 当前开销 | 预期收益 | 源码位置 |
|---|---|---|---|---|---|
| **P0** | findField 消除异常控制流 | 稳态渲染 | 76% CPU（45512ms/60s） | 释放 ~76% Render thread CPU | MolangMappingTree.java:166-170 |
| **P0** | Registry.put 批量化 | 资源重载 + 堆 | 48% 重载 CPU + 149MB 堆 | 释放 ~48% 重载 CPU + 减少堆 | Registry.java:44-47, BedrockAddonRuntimeBridge.java:63-77 |
| **P1** | 正则 Pattern 缓存 | 稳态渲染 | 8.9% CPU（5312ms/60s） | 释放 ~8.9% Render thread CPU | RenderControllerRuntime.java:51 |
| **P2** | ZipFileSystem 生命周期审查 | 堆 | 215MB（5.6M 实例） | 减少 ZIP 索引膨胀 | BrArchive（待定位） |
| **P2** | byte[] 1.16GB 来源定位 | 堆 | 1.16GB（7.9M 实例） | 待 heapdump 确认 | 需 MAT 分析 |

---

## P0-1：findField 消除异常控制流

### 现象
稳态渲染 60s 内，`java.lang.Throwable.fillInStackTrace` 占 **45512ms（76% CPU）**，526 个异常节点。全部来自 `MolangMappingTree.findField`。

### 根因
`MolangMappingTree.java:166-170`：
```java
for (var classData : classes) {
    var aClass = classData.classInstance;
    try {
        return new FieldData(aClass, aClass.getField(fieldName));  // 字段不存在时抛异常
    } catch (NoSuchFieldException ignored) {
        // 未在此类中找到字段；继续尝试其他映射的类
    }
}
```

`Class.getField(fieldName)` 在字段不存在时抛 `NoSuchFieldException`，其构造触发 `fillInStackTrace`（JVM 遍历调用栈填充异常堆栈，开销巨大）。Molang 查询在每帧渲染时对大量不存在的字段名调用 findField（字段不存在是常态——Molang 表达式如 `q.variant`、`q.skin_id` 等会尝试很多可能不存在的字段），每次失败都创建异常。

**对比**：同文件 `findMethod`（line 211）用 `node.actualFunctions.get(methodName)`（Map 查找），不抛异常，无此问题。

### 影响链
findField ← VariantSelector（self 928ms）← MolangRuntimeSupport（self 916ms）← Molang 查询热路径（每帧执行）

### 方案
字段是静态反射结构（类的 public field 集合不变），可在 `addNode` 注册类时预建 `Map<String, Field>`（按名字小写索引），findField 直接 Map.get 返回 null，永不抛异常。

```java
// Node 增加 field 缓存
public final Map<String, FieldData> cachedFields = new HashMap<>();

// addNode 注册类时预建
for (Field f : actualClass.classInstance().getFields()) {
    last.cachedFields.put(f.getName(), new FieldData(actualClass.classInstance(), f));
}

// findField 改为 Map.get
public @Nullable FieldData findField(String name) {
    // ... scopeName/fieldName 解析不变 ...
    Node node = findNode(scopeName);
    return node == null ? null : node.cachedFields.get(fieldName);
}
```

### 预期收益
释放 ~76% Render thread CPU。这是**单点最大收益**的优化，修改局部、风险低。

### 注意事项
- 需确认 Molang 字段名查找是否大小写敏感（当前 `getField` 大小写敏感，但 scopeName 用了 `toLowerCase`）。若 Molang 规范要求大小写不敏感，缓存 key 也应 `toLowerCase`。
- `getFields()` 返回所有 public field（含继承的），与 `getField(name)` 语义一致，缓存覆盖范围正确。

---

## P0-2：Registry.put 批量化

### 现象
资源重载 90s 内，`Registry.put` 占 **43484ms（48.3% CPU）**，其中 `Forge EventBus.doCastFilter` 占 42988ms（47.8%）。堆中 EventBus Lambda 149MB（6.2M 实例）。

### 根因
`Registry.java:44-47`：
```java
public void put(String id, T value) {
    ref.updateAndGet(snap -> snap.with(id, value));  // copy-on-write 全量复制快照
    publisher.publishManagerEntryChanged(managerName, id, value);  // 每条都发事件
}
```

每次 `put` 做两件昂贵的事：
1. **copy-on-write 快照全量复制**：`snap.with(id, value)` 每次重建整个 Map。逐条 put 导致 O(N²) 复制（N 条数据 = N 次全量复制）。
2. **发布 manager 事件**：每条 put 都触发 `publishManagerEntryChanged` → Forge EventBus.post → `doCastFilter`（遍历所有注册的 listener 做类型转换检查）。

`BedrockAddonRuntimeBridge.replaceFromResourcePack` 在资源重载时**逐条 put**：
- 材质（line 63-64）：`value.materials().forEach(MaterialManager.INSTANCE::put);`
- 渲染控制器（line 70-77）：`RenderControllerManager.INSTANCE.put(key, entry);`

**对比**：同方法中实体（line 48）、attachable（line 56）、模型（line 59）已用 `replaceAll` 批量方法，唯独材质和渲染控制器未走批量。材质需保留 vanilla 条目（叠加语义），不能直接 `replaceAll`（会清空），需 `putAll` 合并语义。

### 方案
`Registry` 增加 `putAll` 批量方法（一次 copy-on-write + 一次事件）：

```java
public void putAll(Map<String, T> entries) {
    ref.updateAndGet(snap -> {
        Map<String, T> merged = new HashMap<>(snap.toMap());
        merged.putAll(entries);
        return RegistrySnapshot.copyOf(merged);
    });
    publisher.publishManagerReplaced(managerName);  // 批量事件，而非逐条
}
```

`BedrockAddonRuntimeBridge` 改为收集后批量 put：
```java
// 材质
Map<String, BrMaterial> batch = new HashMap<>();
value.materials().forEach((k, v) -> batch.put(k, v));
MaterialManager.INSTANCE.putAll(batch);
```

### 预期收益
- 释放 ~48% 资源重载 CPU（消除 O(N²) 复制和 N×事件分发）
- 减少 EventBus Lambda 堆分配（149MB → 显著下降）

### 注意事项
- `RegistrySnapshot` 需确认是否有 `toMap()` 或等价方法用于合并。若无，需新增。
- `publishManagerReplaced` 需确认 publisher 是否支持批量事件；若无，可发一次"全量替换"事件替代逐条事件。
- 需确认事件消费者（manager 变更监听器）是否依赖逐条事件语义。若是，批量事件需携带变更集合。

---

## P1：正则 Pattern 缓存

### 现象
稳态渲染 60s 内，`Pattern.compile` 占 **5312ms（8.9% CPU）**。来源 `RenderControllerRuntime.lambda$setup$2`（2408ms）+ `Pattern.<init>`（2224ms）+ `Pattern.sequence`（680ms）。

### 根因
`RenderControllerRuntime.java:51`：
```java
renderController.part_visibility().forEach((k, v) -> {
    if (boneName != null && Pattern.compile(k.replace("*", ".*")).matcher(boneName).matches()) {
        // ...
    }
});
```

嵌套循环（`models` × `allBones` × `part_visibility`）中，对每个 part_visibility 的 key（带 `*` 通配符的 bone 名模式）每次都重新 `Pattern.compile`。part_visibility 的 key 来自渲染控制器定义，是静态数据，正则可预编译。

### 方案
缓存编译后的 Pattern（part_visibility key 集合有限且静态）：

```java
private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

// setup 中
Pattern pattern = PATTERN_CACHE.computeIfAbsent(k.replace("*", ".*"), Pattern::compile);
if (boneName != null && pattern.matcher(boneName).matches()) {
```

或更彻底：在 `RenderControllerEntry` 加载时预编译所有 part_visibility Pattern，存入 entry 结构。

### 预期收益
释放 ~8.9% Render thread CPU。

### 注意事项
- Pattern 线程安全（不可变），ConcurrentHashMap 缓存安全。
- 更优方案：在渲染控制器加载时预编译，避免运行时缓存查找。

---

## P2-1：ZipFileSystem 生命周期审查

### 现象
堆中 `jdk.nio.zipfs.ZipFileSystem$IndexNode` 占 **215MB（5,636,197 实例）**。

### 根因（待定位）
eyelib 的 BrArchive 打开 .mcpack（zip 格式）资源包，每个 zip entry 产生 IndexNode。5.6M 实例说明 zip 索引累积。待审查：
- BrArchive 是否在资源重载后关闭旧的 ZipFileSystem？
- 多个资源包的 ZipFileSystem 是否共享或独立？
- F3+T 重载是否泄漏旧 zip 句柄？

### 方案
定位 BrArchive 的 ZipFileSystem 创建/关闭路径，确保资源重载后旧句柄释放。需读 BrArchive 源码确认（本任务未深入）。

### 预期收益
减少 ~215MB 堆占用（若确认是泄漏）。

---

## P2-2：byte[] 1.16GB 来源定位

### 现象
堆中 `byte[]` 占 **1.16GB（7,960,904 实例）**，是堆最大头（占总堆 ~46%）。平均每个 ~156 字节，实例数远超 String（2M）。

### 局限
spark heapsummary 是 class histogram，不显示引用链。无法直接定位"谁持有 byte[]"。

### 方案
需 `/spark heapdump`（生成 .hprof）+ Eclipse MAT 分析 dominator tree / path to GC roots，定位 byte[] 的持有者。

可能的来源（假设，需验证）：
- 纹理 PNG 解码缓存
- 资源包 zip entry 数据缓冲
- RenderDoc capture 残留（本次 dev 客户端以 RenderDoc capture 模式启动，可能影响堆——需在非 RenderDoc 环境复测确认）

---

## 非问题项（无需行动）

### eyelib 自身堆占用健康
eyelib 全部类合计 **56MB（2.18%）**。Top：Model$Vertex 12.6MB、MolangValue 11.2MB、ConstMolangFunction 6.8MB。数量级合理（每实体每动画多个表达式节点），**无需优化**。

### javac 97MB（/eval 测量伪影）
`com.sun.tools.javac` 类占 97MB，来自 `ScriptEvalService`（/eval HTTP 调试服务）使用 `javax.tools.JavaCompiler` 运行时编译。javac Symbol 表静态持有不释放。**生产环境无 /eval，无此开销**，是基准测试的测量伪影。

### Server thread 开销
T2 附带发现：server-side `spark profiler` 采样显示 eyelib 在服务端几乎零开销（self-time Top 30 唯一 eyelib 函数 = DataAttachmentHelper.getOrCreate，80 单位/60000 = 0.13%）。

---

## 方法论与局限

### 数据可靠性
- 三类 profile 均为 Java sampler（async-profiler 在 dev JVM 不可用，spark 自动 fallback）。Java sampler 有 safepoint bias，但对 fillInStackTrace / Pattern.compile 这类热点检测可靠。
- 所有源码引用已人工核对行号。
- 优化建议的百分比基于 spark 采样数据，是**相对排名**而非绝对值（DAG 压缩导致 self-time 总和可能超 100%）。

### 局限
1. **byte[] 来源未定位**：需 heapdump + MAT，超出 spark 能力。
2. **RenderDoc 影响**：dev 客户端以 RenderDoc capture 模式启动，可能对 CPU/堆有轻微影响（RenderDoc hook 开销）。建议在非 RenderDoc 环境复测确认。
3. **async-profiler 不可用**：dev JVM 环境（Forge 1.20.1 + ModDevGradle）无法加载 async-profiler native agent，只能用 Java sampler。生产环境若能加载 async-profiler，数据精度更高。
4. **单次采样**：每类只做一次 profile。严格基准应多次采样取均值，但本任务目标是识别瓶颈（非精确基准），单次足够。

### 测试环境
- MC 1.20.1 Forge 47.1.3，Java 17，Stonecutter active=1.20.1
- eyelib 21.1.14（dev build）
- Actions-and-Stuff mcpack（113 实体定义）作为测试数据
- Windows，RenderDoc capture 模式启动
