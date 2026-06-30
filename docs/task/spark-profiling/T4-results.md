# T4 堆内存 Heapsummary 结果

## 测试条件
- 命令：server-side `/spark heapsummary`（client-side `/sparkc heapsummary` 未输出到 chat；server-side 通过 integrated server 执行，分析整个 JVM 堆——client+server 同进程）。
- 执行路径：`server.getCommands().performPrefixedCommand(host.createCommandSourceStack().withPermission(4), "spark heapsummary")`，返回 1。
- URL 获取：读 `versions/1.20.1/run/config/spark/activity.json`（记录所有 spark 活动，含 heap dump summary URL）。
- 负载：59 eyelib slime + 12 sheep（T2/T3 负载仍在）+ 本任务累积的 /eval 调试开销。
- heap URL：https://spark.lucko.me/8RhM134XVh
- 数据：`data/t4-heapsummary-full.json`（4.15MB，`?raw=1&full=true` 才含 entries；`?raw=1` 只有 metadata）。33936 个 class 条目。

## 堆概况
- 堆 used：2.8GB（metadata），entries sum：2.52GB（class histogram 合计，不含 class 开销外的对齐/padding）。
- GC：G1 Young 20778 次（avg 2ms），G1 Old 2 次（avg 485ms）。老年代压力低。

## 核心发现（按 size 排序）

### 1. byte[] 1.16GB（7,960,904 实例）—— 堆最大头
- 平均每个 ~156 字节。实例数（7.9M）远超 String（2M），说明不是纯字符串。
- 可能来源：纹理解码缓存、资源包 zip entry 缓冲、或 JNI 分配。
- **spark heapsummary 不显示引用链，需 heapdump（MAT/JFR）进一步定位**，超出 spark 能力范围。

### 2. jdk.nio.zipfs.ZipFileSystem$IndexNode 215MB（5,636,197 实例）
- ZIP 文件系统索引节点膨胀。eyelib 的 BrArchive 打开 .mcpack（zip 格式）资源包，每个 zip entry 产生 IndexNode。
- 5.6M 实例说明 zip 索引累积（资源包条目 × 打开的 zip 句柄）。
- 待查：BrArchive 是否及时关闭 ZipFileSystem，或索引是否可共享。

### 3. Forge EventBus Lambda 149MB（两个 Lambda 各 3,102,148 实例）
- `EventBus$$Lambda$4704` 99MB + `EventBus$$Lambda$4703` 49MB，实例数完全相同（3,102,148）。
- 呼应 T3 发现：`EventBus.doCastFilter` 占重载期 47.8% CPU。这两个 Lambda 是事件分发的 cast filter，每次事件 post 创建临时对象。
- 与 Registry.put 每次发事件直接相关（T3 根因）。

### 4. com.sun.tools.javac 97MB —— /eval 调试测量伪影（非生产问题）
- `com.sun.tools.javac.util.List` 19MB、`Symbol$ClassSymbol` 15MB、`Symbol$MethodSymbol` 13MB 等。
- 来源：`io.github.tt432.eyelib.common.debug.ScriptEvalService`（/eval HTTP 服务）使用 `javax.tools.JavaCompiler`（JDK javac）运行时编译代码片段。
- **javac 的 Symbol 表是静态持有的，编译过的类符号不释放**（javac 已知特性）。本任务执行了几十次 /eval，累积 97MB javac 对象。
- **这是基准测试的测量伪影，不是 eyelib 生产代码的问题**。生产环境无 /eval，无此开销。

### 5. ✅ eyelib 自身堆占用健康：56MB（2.18%）
Top eyelib 类：
| 类 | size | 实例数 |
|---|---|---|
| model.Model$Vertex | 12.6MB | 525,304 |
| molang.MolangValue | 11.2MB | 466,541 |
| molang.MolangValue$ConstMolangFunction | 6.8MB | 425,261 |
| animation.bedrock.BrBoneKeyFrame | 4.5MB | 141,499 |
| molang.MolangValue3 | 3.5MB | 144,430 |
| animation.bedrock.BrBoneKeyFrameDefinition | 3.4MB | 141,499 |
| model.Model$Face | 3.2MB | 131,326 |
| molang.type.MolangFloat | 2.8MB | 172,583 |

- MolangValue + ConstMolangFunction + MolangValue3 合计 ~21MB，是 Molang 表达式 AST 节点。数量级合理（每实体每动画多个表达式）。
- Model$Vertex/Face/Bone 是几何数据，数量与 mcpack 模型复杂度匹配。
- **eyelib 堆无需优化**。

## 优化建议（T5 详述）
1. **byte[] 1.16GB**：需 heapdump（`/spark heapdump` + MAT）定位来源，spark heapsummary 无法显示引用链。优先级高（占总堆 46%）。
2. **ZipFileSystem IndexNode 215MB**：审查 BrArchive 的 ZipFileSystem 生命周期，确保资源重载后旧 zip 句柄释放。
3. **EventBus Lambda 149MB**：与 T3 的 Registry.put 批量化优化同源——减少事件发布频次即可减少 Lambda 创建。
4. **eyelib 堆健康**：无需行动。

## 方法论说明
- heapsummary 是 class histogram（按类聚合的实例数 + 字节数），**不显示对象引用链**。
- 要定位"谁持有 byte[]"，需 heapdump（完整堆转储）+ MAT/dominator tree 分析。
- spark 也支持 `/spark heapdump`，但生成的 .hprof 文件需外部工具（MAT）分析，超出本任务范围。
