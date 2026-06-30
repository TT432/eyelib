# spark-profiling 优化执行规划

> 依据：`../VERIFICATION-RESULT.md`（已逐行核对源码）。本文档是"执行优化"阶段的方案与决策记录。
> 子任务过程产物放在本目录（`optimization/`）下各子任务名子目录。

## 源码核实结论（对 VERIFICATION-RESULT.md 的增补）

VERIFICATION-RESULT.md 判定绝大部分准确，执行阶段补充核实两处：

1. **V3-d 文件位置订正**：`matchBonePattern` 实际在 `RenderControllerEntry.java:452-473`，**不在** `RenderControllerRuntime.java`（该文件仅 58 行）。判定方向正确（startsWith 是更优替代），但"同文件"表述错。
2. **P0-2 事件订阅者核实（VERIFICATION 未覆盖）**：全仓 `ManagerEntryChangedEvent` 共 3 个订阅者：
   - `CapabilityComponentRuntimeHooks.onManagerEntryChanged:34` → `AnimationComponent.onManagerEntryChanged:57`，**仅当 `AnimationLookup.managerName().equals(managerName)` 才处理**（动画管理器专属）。
   - `RenderHelper:58`，**仅处理 `ModelManager`**（按 entryName 失效 dfsModels 缓存）。
   - `ModelBakeInvalidationHooks:25`，**仅处理 `ModelManager`**（按 entryName 失效烘焙缓存）。
   - **结论**：三者均不关心 `MaterialManager` / `RenderControllerManager`。材质/RC 的 put/putAll 发何种事件、甚至不发，对现有订阅者零影响。putAll 事件方案安全。

## 三个执行子任务

### Opt1（P0-1，高收益低风险）：findField 消除异常控制流

**目标文件**：`src/main/java/io/github/tt432/eyelib/molang/mapping/api/MolangMappingTree.java`

**事实**：`findField`（148-174）循环 163-171 用 `aClass.getField(fieldName)` + `catch NoSuchFieldException`。字段不存在是常态（每帧大量 `q.variant` 等不存在字段查询），每次失败 `fillInStackTrace` 占稳态渲染 76% CPU（45512ms/60s）。生产唯一调用点 `MolangRuntimeSupport.resolveMemberAccess:52`。

**方案**：
- `Node`（86-90）加 `public final Map<String, FieldData> cachedFields = new HashMap<>();`
- `addNode`（92-115）注册类后，遍历 `actualClass.classInstance().getFields()`，对每个 field 执行 `cachedFields.putIfAbsent(f.getName(), new FieldData(actualClass.classInstance(), f))`。
- `findField` 改为：解析 scopeName/fieldName（不变）→ `findNode(scopeName)` → 返回 `node == null ? null : node.cachedFields.get(fieldName)`。

**关键决策**：
- **多 MolangClass 字段冲突**：`Node.actualClasses` 是 List，可注册多个类。原 findField 按 actualClasses 顺序遍历，**先注册的类优先**。缓存用 `putIfAbsent` 保持该语义（后注册类的同名字段不覆盖先注册类的）。
- **大小写**：scopeName 经 `toLowerCase`，fieldName **大小写敏感**（`getField` 是 JVM 原生大小写敏感）。缓存 key 保持 fieldName 原样，**不可 toLowerCase**（VERIFICATION V1-g）。
- `Class.getFields()` 返回所有 public field（含继承），与 `getField(name)` 查找范围一致，覆盖正确。

### Opt2（P1，中等收益低风险）：Pattern 替换为 startsWith 语义

**目标文件**：`src/main/java/io/github/tt432/eyelib/client/entity/RenderControllerRuntime.java`

**事实**：`setup`（44-57）第 51 行 `Pattern.compile(k.replace("*", ".*")).matcher(boneName).matches()` 在三层嵌套循环里反复编译正则，占稳态渲染 8.9% CPU（5312ms/60s）。`import java.util.regex.Pattern` 在第 16 行。

**方案决策（选择 startsWith，非 Pattern 缓存）**：
- 同项目 `RenderControllerEntry.matchBonePattern:452-473` 已为 Bedrock bone 名模式建立标准实现：`*` 单独出现 = 全部；`xxx*` = 前缀；无 `*` = 精确。
- Bedrock 规范的 part_visibility bone 模式就是前缀/全部语义，中间带 `*` 非规范支持。
- RenderControllerRuntime.setup 处理的是同一份 part_visibility 数据，语义应与 matchBonePattern 对齐。
- **决策**：内联 startsWith 逻辑（3 行），彻底消除 Pattern.compile 及 matcher 分配。移除 `import java.util.regex.Pattern`。
- **不选 Pattern 缓存的原因**：缓存只省编译开销，仍保留 matcher 分配；startsWith 零分配，收益更大且与项目标准一致。
- 该决策不构成"静默简化"：Bedrock * 规范本就是前缀/全部，与 matchBonePattern 既有实现一致。

### Opt3（P0-2，高收益中等风险）：Registry.putAll 批量化

**目标文件**：
- `util/manager/ManagerEventPublisher.java`（加 default 方法）
- `util/manager/ManagerEventPublishBridge.java`（加转发）
- `bridge/client/manager/ForgeManagerEventPublisher.java`（覆盖）
- `bridge/event/ManagerReplacedEvent.java`（新增）
- `util/repository/Repository.java`（加 default putAll）
- `util/registry/Registry.java`（覆盖 putAll）
- `client/loader/BedrockAddonRuntimeBridge.java`（材质/RC 改批量）

**事实**：`Registry.put`（44-47）每次 copy-on-write 全量复制（O(N²)）+ 发 1 次 `publishManagerEntryChanged`（Forge `doCastFilter` 占重载 47.8% CPU、149MB 堆）。`BedrockAddonRuntimeBridge`（61-79）对材质（line 64）和渲染控制器（line 76）逐条 put，是仅有的两处未批量化热点（实体/attachable/模型已 replaceAll）。

**方案**：
1. `ManagerEventPublisher` 加 `default void publishManagerReplaced(String managerName) {}`（保留 @FunctionalInterface 与 lambda/NOOP 兼容）。
2. `ManagerEventPublishBridge` 加 `public static void publishManagerReplaced(String managerName)` 转发。
3. 新增 `ManagerReplacedEvent extends Event`（字段：managerName），放 `bridge/event/`。
4. `ForgeManagerEventPublisher` 覆盖 `publishManagerReplaced`，post `ManagerReplacedEvent`。
5. `Repository` 加 `default void putAll(Map<String, ? extends T> entries) { entries.forEach(this::put); }`（默认逐条，向后兼容）。
6. `Registry` 覆盖 `putAll`：一次 `updateAndGet`（`new LinkedHashMap<>(snap.all())` + `putAll(entries)` + `copyOf`）+ 一次 `publishManagerReplaced(managerName)`。空 entries 提前 return 不发事件。
7. `BedrockAddonRuntimeBridge`：
   - 材质（61-66）：收集 `Map<String, BrMaterialEntry> batch`，`MaterialManager.INSTANCE.putAll(batch)`。
   - 渲染控制器（68-79）：保留 part_visibility size 过滤逻辑，通过过滤的收集到 `Map<String, RenderControllerEntry> batch`，`RenderControllerManager.INSTANCE.putAll(batch)`。

**预期收益**：消除材质/RC 的 O(N²) 复制 + N×事件分发（T3 显示两者合计 ~48% 重载 CPU + 149MB EventBus Lambda 堆）。

**BrMaterial 类型**：`material.material.BrMaterial` 是 record(`Map<String, BrMaterialEntry> materials`)。材质 batch 类型 `Map<String, BrMaterialEntry>`，`MaterialManager.INSTANCE` 是 `Registry<BrMaterialEntry>`。

## 不执行项（超出范围 / 方向错误）

- **P2-1 ZipFileSystem**：方向错误（VERIFICATION V4）。eyelib 用 `java.util.zip.ZipFile`，不产生 NIO `ZipFileSystem$IndexNode`；215MB IndexNode 应在 Forge/Vanilla/其他 mod 排查。文档改正移出 eyelib 优化范畴。
- **P2-2 byte[] 1.16GB**：需 heapdump + MAT，超出 spark 能力。文档保留为后续项。

## 文档改正子任务（Docs）

按 VERIFICATION-RESULT.md "改正范围"逐项改正：
- `FINAL-REPORT.md`：V1（影响链/行号/大小写）、V2（toMap→all、publishManagerReplaced 不存在→改为本次新增、replaceAll 不发事件）、V3（matchBonePattern 文件位置订正）、V4（BrArchive 整条重写）、V5-d（T3 局限）。
- `PLAN.md`（任务规划）：V5-a/b/c（命令前缀 / --thread 风险描述）。
- `T2-results.md`：V1（影响链 + 行号）。
- `T3-results.md`：V5-d（采样方法局限声明）。
- `T4-results.md`：V4（BrArchive → BedrockAddonLoader + 非 eyelib 产生）。

## 验证子任务（Verify）

- `eyelib_debug_build`（version=1.20.1）构建。
- `eyelib_debug_test`（version=1.20.1）跑全量单测（含 Molang/Registry/BedrockAddonRuntimeBridge 相关测试）。
- 必要时 `eyelib_debug_nullaway` 确认 null 安全。
