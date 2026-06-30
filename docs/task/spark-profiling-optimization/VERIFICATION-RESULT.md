# spark-profiling 结果逐项验证

> 任务：把 FINAL-REPORT.md 与各 T*-results.md 中的每条代码声明（行号、调用链、根因、方案可行性）对照真实源码核对，改正错误。
> 验证日期：2026-06-30。本文件是改正各文档的依据。

## 验证结论汇总

| 项 | 声明 | 判定 | 改正动作 |
|---|---|---|---|
| V1-a | findField 影响链 `findField ← VariantSelector ← MolangRuntimeSupport` | ❌ | 改正：实际 `MolangRuntimeSupport.resolveMemberAccess:52` 直调 findField，**不经 VariantSelector** |
| V1-b | findField 在 MolangMappingTree.java:166-170 | ⚠️ | 改正：方法 148-174，循环 163-171，166-170 仅是 try 块 |
| V1-c | catch 块为空 | ⚠️ | 改正：catch 块含中文注释 |
| V1-d | 循环变量类型 ClassData | ❌ | 改正：实际类型为 record `MolangClass` |
| V1-e | findMethod 行 211 用 Map 查找 | ✅ | 无需改 |
| V1-f | cachedFields 方案可行性 | ✅ | 补充"多 MolangClass 注册到同一 Node 时字段名冲突"注意点 |
| V1-g | 大小写处理 | ⚠️ | 改正：scopeName 小写化，**fieldName 大小写敏感**，缓存 key 不可 toLowerCase |
| V2-a | Registry.put:44-47 / replaceAll:50-52 | ✅ | 补充：replaceAll **不发事件**（对 putAll 方案有影响） |
| V2-b | RegistrySnapshot.toMap() 存在 | ❌ | 改正：方法叫 `all()`，返回不可变 Map 视图 |
| V2-c | publishManagerReplaced 存在 | ❌ | 改正：不存在，需新增（破坏 @FunctionalInterface） |
| V2-d | BedrockAddonRuntimeBridge 行号 | ✅ | 微调：材质 forEach 实际在 line 64，渲染控制器 put 在 line 76 |
| V2-e | putAll 方案可直接落地 | ❌ | 改正：需新增 2 处 API（`ManagerEventPublisher.publishManagerReplaced` + bridge） |
| V3-a | Pattern.compile 在 RenderControllerRuntime:51 | ✅ | 无需改 |
| V3-b | lambda$setup$2 / 三层嵌套 / Map<String, MolangValue> | ✅ | 无需改 |
| V3-c | PATTERN_CACHE 用 ConcurrentHashMap | ⚠️ | 补充：需 `import java.util.concurrent.ConcurrentHashMap`，或考虑单线程 HashMap |
| V3-d | "更彻底方案"预编译 | ⚠️ | 补充：同文件 `matchBonePattern:452-473` 已用 `startsWith` 实现 `*` 通配的零分配等价语义，是更优替代 |
| V4-a | eyelib 的 BrArchive 打开 .mcpack | ❌❌ | 改正：BrArchive 不存在；打开 .mcpack 的是 `BedrockAddonLoader.collectFilesFromZip():1054` |
| V4-b | eyelib 创建 ZipFileSystem$IndexNode | ❌❌ | 改正：eyelib 用 `java.util.zip.ZipFile`，**不创建 NIO ZipFileSystem$IndexNode**。两类是不同 JDK 实现 |
| V4-c | 审查 BrArchive 生命周期可减 215MB | ❌ | 改正：方向错误。IndexNode 来源应在 Forge/Vanilla 资源包系统或其他 mod 排查 |
| V5-a | PLAN.md "T2/T3/T4 统一用 /spark，不存在 /sparkc" | ❌ | 改正：实际全部用 `/sparkc`；`/sparkc` 是 Forge 客户端命令，确实存在 |
| V5-b | PLAN.md 第 2-5 节 `/spark` + `--thread "Render-Thread"` 设计 | ⚠️ | 加修订标注：T1 后已切换到 `/sparkc`、不带 `--thread`（见 tooling-notes.md） |
| V5-c | PLAN.md 风险表 `--thread "Render-Thread"` "样本太少" | ❌ | 改正：真实原因是引号被 brigadier 吞掉→`threadDumper.ids=[]`→采空，不是"样本太少" |
| V5-d | T3 实际采样方法 vs PLAN 设计 | ❌ | PLAN 设计全线程；T3 实际只采 Render thread → T3 数据**系统性低估** reload 开销（worker 线程未采），需在 T3-results 与 FINAL-REPORT 显式声明此局限 |

## 改正范围

- **VERIFICATION-RESULT.md**（本文件）：留档
- **FINAL-REPORT.md**：V1（影响链+行号+大小写）、V2（toMap→all、publishManagerReplaced 不存在、replaceAll 不发事件）、V3（ConcurrentHashMap import、matchBonePattern 替代）、V4（BrArchive 整条重写）、V5-d（T3 局限）
- **PLAN.md**：V5-a/b/c（修订命令前缀、--thread 风险描述）
- **T2-results.md**：V1（影响链 + 行号 + 循环类型）
- **T3-results.md**：V5-d（采样方法局限声明）
- **T4-results.md**：V4（BrArchive → BedrockAddonLoader + 非 eyelib 产生）
- **tooling-notes.md**：本身已准确，不改

## 详细验证记录

### V1：P0-1 findField（MolangMappingTree.java）

**实际源码**（`MolangMappingTree.java:148-174`）：

```java
public @Nullable FieldData findField(String name) {
    int i = name.indexOf(".");
    String fieldName;
    String scopeName;
    if (i != -1) {
        scopeName = name.substring(0, i).toLowerCase(Locale.ROOT);
        fieldName = name.substring(i + 1);
    } else {
        scopeName = "";
        fieldName = name;
    }
    Node node = findNode(scopeName);
    List<MolangClass> classes = node == null ? List.of() : node.actualClasses;
    for (var classData : classes) {
        var aClass = classData.classInstance;
        try {
            return new FieldData(aClass, aClass.getField(fieldName));
        } catch (NoSuchFieldException ignored) {
            // 未在此类中找到字段；继续尝试其他映射的类
        }
    }
    return null;
}
```

**真实调用图**（grep 全仓 `findField(`）：
- 生产代码调用点**唯一**：`MolangRuntimeSupport.resolveMemberAccess():52` → `mappingTree.findField(dottedName)`
- 测试代码：`MolangMcAdapterSeamTest.java:46,52`

**VariantSelector 与 findField 的真实关系**：VariantSelector.java 全文零次调用 `findField`，它只调 `tree.findMethod(name)`（VariantSelector.java:30）。报告把两条独立路径（字段解析 vs 方法变体选择）混淆。

**大小写处理**：scopeName 经 `.toLowerCase(Locale.ROOT)`（line 153），**fieldName 未做 toLowerCase**（line 154）。`Class.getField(name)` 是 JVM 原生大小写敏感。报告"缓存 key 也应 toLowerCase"的注意事项**方向错误**——若缓存 key toLowerCase 而 `getField` 大小写敏感，会破坏语义；缓存 key 必须保持原样（大小写敏感）。

**循环变量类型**：`MolangClass` 是 record（line 42 附近），不是 `ClassData`。报告 P0-1 节里代码片段注释虽未直说类型名，但"对比"段提及 "实际类型为 ClassData"——这是 V1 子代理推断的，原文报告未出现 ClassData 字样。**实际无需改这一条**（报告未声称类型名）。

**findMethod 对比**：`MolangMappingTree.java:211` `node.actualFunctions.get(methodName)` ✅ 准确。

**cachedFields 方案可行性**：
- `addNode(String name, MolangClass actualClass)` 存在（line 92）
- `FieldData(Class<?> clazz, Field field)` record 签名匹配（line 141-145）
- `Node` 是 `public static class`（line 86-90）
- `Class.getFields()` 返回所有 public 字段（含继承），与 `getField(name)` 查找范围一致，方案覆盖正确
- **新增注意点**：`Node.actualClasses` 是 `List<MolangClass>`，可注册多个类到同一 Node（`addNode` 用 `equals` 去重，line 101-105）。预建缓存时需合并多个类的字段；后注册的类若字段名冲突，需定义策略（覆盖 / 保留首个 / 全列）。

### V2：P0-2 Registry.put（Registry.java）

**Registry.put 实际源码**（`Registry.java:43-47`）：
```java
@Override
public void put(String id, T value) {
    ref.updateAndGet(snap -> snap.with(id, value));
    publisher.publishManagerEntryChanged(managerName, id, value);
}
```
行号 ✅ 匹配。

**Registry.replaceAll 实际源码**（`Registry.java:50-52`）：
```java
public void replaceAll(Map<String, ? extends T> replacement) {
    ref.set(RegistrySnapshot.copyOf(replacement));
}
```
**关键事实（报告未提）**：`replaceAll` **不发布任何事件**。这与 `put`（每条发事件）形成对比，对 putAll 设计影响重大——若 putAll 想发"全量替换"事件，需先新增方法。

**RegistrySnapshot 实际 API**：
- `with(String id, T value)` → 新快照（package-private, line 59-63）✅
- `copyOf(Map<String, ? extends T> source)` → 静态（package-private, line 30-36）✅
- `all()` → 返回内部不可变 `Map<String, T>` 视图（line 43-45）✅
- **`toMap()` 不存在** ❌

**ManagerEventPublisher（@FunctionalInterface）实际 API**：
```java
void publishManagerEntryChanged(String managerName, String entryName, Object entryData);
```
仅此一个抽象方法 + `NOOP` 常量。**`publishManagerReplaced` 不存在** ❌。`ManagerEventPublishBridge` 也只有 `publishManagerEntryChanged` 转发。

**putAll 方案落地评估**：
- `snap.toMap()` → 改为 `new LinkedHashMap<>(snap.all())`（all 返回不可变视图，需拷贝可变副本用于合并）
- `publisher.publishManagerReplaced(managerName)` → 不存在，需新增：
  1. `ManagerEventPublisher` 接口加方法（破坏 @FunctionalInterface，需改为普通接口或保留 lambda 兼容用 default method）
  2. `ManagerEventPublishBridge` 加 static 转发
- 结论：方案**不可直接落地**，需新增 2 处 API

**BedrockAddonRuntimeBridge.replaceFromResourcePack**（line 41-79）行号核对：
| 项 | 报告行号 | 实际 | 备注 |
|---|---|---|---|
| 实体 replaceAll | 48 | 48 | ✅ |
| attachable replaceAll | 56 | 56 | ✅ |
| 模型 replaceAll | 59 | 59 | ✅ |
| 材质逐条 put | 63-64 | for 在 63，forEach 在 64 | ✅（写法"forEach 在 line 64"更精确） |
| 渲染控制器逐条 put | T3 说 76 / FINAL-REPORT 说 70-77 | for 在 70，put 在 76 | ✅ |

### V3：P1 Pattern 缓存（RenderControllerRuntime.java）

**setup 方法（44-57）实际源码**：
```java
public void setup(Collection<Model> models, RenderControllerEntry renderController) {
    Int2ObjectOpenHashMap<ReferenceList<MolangValue>> part = new Int2ObjectOpenHashMap<>();
    partVisibility = part;
    models.stream().filter(java.util.Objects::nonNull).forEach(model -> {
        model.allBones().int2ObjectEntrySet().forEach(entry -> {
            String boneName = GlobalBoneIdHandler.get(entry.getIntKey());
            renderController.part_visibility().forEach((k, v) -> {
                if (boneName != null && Pattern.compile(k.replace("*", ".*")).matcher(boneName).matches()) {
                    part.computeIfAbsent(entry.getIntKey(), __ -> new ReferenceArrayList<>()).add(v);
                }
            });
        });
    });
}
```
行号 51 ✅；lambda$setup$2 ✅（setup 有 3 个 lambda，$2 是 part_visibility）；三层嵌套 ✅。

`RenderControllerEntry.part_visibility()` 返回 `Map<String, MolangValue>` ✅（key 是带 `*` 的 bone 名匹配式）。

**PATTERN_CACHE 方案补充**：
- `Pattern` 已 import（line 16）
- `ConcurrentHashMap` **未 import**，方案需补 `import java.util.concurrent.ConcurrentHashMap;`
- 渲染线程单线程上下文，用 `HashMap` 即可，无需 ConcurrentHashMap（设计选择，非错误）

**更优替代（报告遗漏）**：同文件 `RenderControllerRuntime.matchBonePattern():452-473` 已实现 `*` 通配的零分配等价语义：
```java
boolean isPrefix = pattern.endsWith("*");
String lookup = isPrefix ? pattern.substring(0, pattern.length() - 1) : pattern;
boolean match = pattern.equals("*") || lookup.isEmpty()
    || (isPrefix ? boneName.startsWith(lookup) : boneName.equals(lookup));
```
应作为 P1 首选方案（彻底消除 Pattern.compile），缓存 Pattern 是次选。

### V4：P2-1 BrArchive —— **整条建议方向错误**

**事实**：
1. **BrArchive 类不存在**。报告承认"待定位"，实际无此类。最接近的是 `BrArchiveDecoder`（解码 `.brarchive` 二进制存档，非 zip）。
   - 补充：`BrArchive` 在源码中是**格式名/概念**（指 `.brarchive` 二进制存档格式），出现在 `BedrockAddonLoader` 的注释和方法名中——`loadBrarchive():428` 调 `BrArchiveDecoder.extractJson` 处理 `.brarchive` 格式。**作为类名不存在，作为格式概念存在**。
2. **eyelib 不创建 `jdk.nio.zipfs.ZipFileSystem$IndexNode`**。全仓 grep 0 匹配 `FileSystems.newFileSystem` / `ZipFileSystem`。eyelib 用的是 `java.util.zip.ZipFile`（完全不同的 JDK API），不产生 NIO ZipFileSystem 的 IndexNode。
3. **实际打开 .mcpack 的位置**：`src/main/java/io/github/tt432/eyelib/importer/addon/BedrockAddonLoader.java:1054` `collectFilesFromZip()`：
   ```java
   ZipFile zf = new ZipFile(source.toFile());
   openZips.add(zf);
   ```
4. **生命周期正确**：`BedrockAddonLoader.load()` 的 finally 块（line 84-91）统一关闭所有 ZipFile，不跨调用泄漏。
5. **`SharedLibraryLoader.readFile():110`** 有一个无关的 ZipFile 未关闭小泄漏，不在 reload 路径，与 IndexNode 无关。

**改正结论**：P2-1 整条建议（"审查 BrArchive ZipFileSystem 生命周期可减 215MB"）基于错误假设，应**整条重写**为：
- eyelib 不产生 ZipFileSystem$IndexNode（用的是 ZipFile，生命周期正确）
- 215MB IndexNode 应在 **Forge/Vanilla 资源包系统或其他 mod** 中排查
- P2-1 优先级从 "审查 eyelib" 降级，移出"eyelib 优化"范畴

### V5：文档自洽性

**V5-a/b**：PLAN.md 第 2 节（line 18-23）写的 `/spark` + `--thread "Render-Thread"` 是规划阶段猜想，T1 执行后已用 `/sparkc` + 不带 `--thread` 推翻（见 tooling-notes.md）。PLAN.md line 112 又重复"统一用 /spark（不存在 /sparkc）"——这句**双重错误**：实际上 T2/T3 全部用了 `/sparkc`，且 `/sparkc` 在 Forge 客户端确实存在（client-side 命令）。

**V5-c**：PLAN.md line 69 风险表说 `--thread "Render-Thread"` "过滤后样本太少 → 改用全线程 `*`"。**真实原因**（见 T2-results.md line 14、tooling-notes.md line 76）是：引号在 `ClientCommandHandler.runCommand` 里被 brigadier 解析吞掉，导致 `threadDumper.ids=[]`，**采样完全为空**（protobuf 6KB，threads 字段不存在），而非"样本太少"。

**V5-d**：PLAN.md line 43 Benchmark B 设计要求**全线程**采样（`/spark profiler start --timeout 90`，因 reload 涉及多线程）。T3-results.md line 4 实际："线程：Render thread（`/sparkc profiler`，无 --thread）"——**只采了 Render thread**，违反设计意图。reload 大量工作（codec 解析、`.mcpack` 解压、worker 线程上的 SimpleJsonResourceReloadListener）发生在 worker 线程，T3 数据**系统性低估**真实 reload 开销。FINAL-REPORT.md 与 T3-results.md 均未声明此局限。
