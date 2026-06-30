# spark Profiling 工具演化笔记

> 记录本次任务中验证可用的 spark 集成与数据采集流程，供后续 eyelib 性能基准测试复用。

## 1. spark 集成（build.gradle）

### 方案：CurseMaven + modLocalRuntime

spark 是 SRG 名映射的 Forge mod，必须经过 ModDevGradle 的 remap pipeline 才能在 dev 环境运行。

**build.gradle 改动**（2 处）：

1. 加 CurseMaven repo（与已有 Modrinth repo 并列）：
```groovy
exclusiveContent {
    forRepository { maven { url = "https://cursemaven.com" } }
    filter { includeGroup "curse.maven" }
}
```

2. 加 spark 依赖（`isLegacyForge` 条件下，即 1.20.1 节点）：
```groovy
modLocalRuntime "curse.maven:spark-361579:4738952"
```
- `spark-361579`：CurseForge project ID（descriptor 自定义，只需唯一）
- `4738952`：file ID = spark 1.10.53（1.20.1 forge 唯一兼容版本）
- **必须用 `modLocalRuntime`**（不是 `localRuntime`）：只有 `modLocalRuntime` 触发 SRG→Mojmap remap。`localRuntime` 直接用 SRG jar，spark 因 `NoSuchMethodError` 崩溃。

### 版本约束
- spark 1.10.53 = 1.20.1 forge 唯一兼容版本（file 4738952）。
- Modrinth 的 `1.10.173-forge` 是 NeoForge/高版本专用，不兼容 1.20.1。
- 验证 file ID：CurseMaven test endpoint `https://cursemaven.com/test/361579/<fileId>`。

### 失败方案（勿用）
- 裸 jar 投 `run/mods/` 或 `versions/<ver>/run/mods/`：不 remap，`NoSuchMethodError`。
- `localRuntime "..."`：不 remap，同上。
- 手工 remap（ForgeAutoRenamingTool）：对 spark jar module-info 越界 + 内部 class signature NPE。

---

## 2. sparkc profiler 执行配方

### 关键区分
- **`/sparkc`** = Forge/Fabric **客户端**命令（采 client 线程，如 Render thread）。
- **`/spark`** = server-side 命令（默认只采 Server thread）。
- T2 目标是 Render thread CPU → 必须用 `/sparkc`。

### 执行路径（/eval 反射）
```java
// 启动 profiler（不加 --thread，默认采 client 主线程 = Render thread）
Class<?> cch = Class.forName("net.minecraftforge.client.ClientCommandHandler");
Method runCmd = cch.getMethod("runCommand", String.class);
Boolean r = (Boolean) runCmd.invoke(null, "sparkc profiler start --timeout 60");
// r=true 表示命令被接受

// 等 timeout 上传后，获取 URL
runCmd.invoke(null, "sparkc activity");
// 反射读 allMessages 拿 URL
```

### 拿 URL：读 ChatComponent.allMessages
```java
java.lang.reflect.Field f = minecraft.gui.getChat().getClass().getDeclaredField("allMessages");
f.setAccessible(true);
java.util.List msgs = (java.util.List) f.get(minecraft.gui.getChat());
// GuiMessage 无 getMessage()，用 toString() 提取（URL 在 content literal 里）
for (int i = 0; i < Math.min(msgs.size(), 3); i++) {
    result += msgs.get(i).toString();
}
```

### 更可靠的 URL 路径：activity.json
读 `versions/<mc-version>/run/config/spark/activity.json`（spark 自动写入的 JSON 存档，含所有活动的 URL）。不依赖 chat appender（日志在暂停后可能停止刷新）。

### 踩坑
- **不要加 `--thread "Render thread"`**：引号在 ClientCommandHandler.runCommand 里被 brigadier 吞掉，导致 `threadDumper.ids=[]`，采样数据为空（protobuf 仅 ~6KB，threads field 不存在）。
- **client-side `/sparkc heapsummary` 无 chat 输出**：改用 server-side `/spark heapsummary`。
- **单机世界失焦暂停**：进入世界后 `pauseOnLostFocus=true` 导致 Render thread 冻结，profiler 采不到数据。执行前先 `mc.options.pauseOnLostFocus = false` + `mc.setScreen(null)`。

---

## 3. protobuf 解析配方

### raw 数据下载
```powershell
# metadata（小）
curl.exe -s "https://spark.lucko.me/<id>?raw=1" -o metadata.json
# 完整 protobuf 火焰图（大，存为 .bin）
curl.exe -s "https://spark-usercontent.lucko.me/<id>" -o profile.bin
# heapsummary 完整 entries
curl.exe -s "https://spark.lucko.me/<id>?raw=1&full=true" -o heap-full.json
```

### Java API 解析（/eval 内，proto 类已在 classpath）
```java
byte[] data = java.nio.file.Files.readAllBytes(
    java.nio.file.Paths.get("docs/task/spark-profiling/data/<file>.bin"));
me.lucko.spark.proto.SparkSamplerProtos.SamplerData sd =
    me.lucko.spark.proto.SparkSamplerProtos.SamplerData.parseFrom(data);

java.util.List threads = sd.getThreadsList();
// ThreadNode: getChildrenList() = 扁平节点池, getChildrenRefsList() = 根栈帧的池 index
// StackTraceNode: getClassName(), getMethodName(), getTimesList(), getChildrenRefsList()
// 反射类名: Class.forName("...$ThreadNode") / Class.forName("...$StackTraceNode")
//   （不能用 SamplerData.ThreadNode，javap 确认内部类名用 $ 分隔）
```

### protobuf 字段映射（javap 确认）
SamplerData: 1=METADATA, 2=THREADS, 3=CLASS_SOURCES, 4=METHOD_SOURCES, 5=LINE_SOURCES, 6=TIME_WINDOWS, 7=TIME_WINDOW_STATISTICS(map), 8=CHANNEL_INFO

ThreadNode: NAME(1), CHILDREN(3, 扁平节点池), TIMES(DoubleList), CHILDREN_REFS(IntList, 根引用)

StackTraceNode: CLASS_NAME(3), METHOD_NAME(4), PARENT_LINE_NUMBER(5), LINE_NUMBER(6), METHOD_DESC(7), TIMES(DoubleList), CHILDREN_REFS(IntList)。**无内嵌 children**，用 childrenRefs 引用节点池 → DAG 压缩结构。

---

## 4. 分析脚本清单

`scripts/` 目录下的 spark 分析脚本：

| 脚本 | 用途 |
|---|---|
| `spark_proto_decode.py` | raw protobuf decoder（递归解析 length-delimited，探测顶级字段） |
| `analyze_render_profile.py` | 算 self-time / total-time，输出 Top-N + eyelib 分类。输入 CSV（Java API 导出的 stacknodes） |
| `trace_hotspots.py` | 构建反向 parent 索引，追溯热点（如 fillInStackTrace）的业务调用者 |
| `probe_pb.py` | raw protobuf 顶级 field 探测（快速判断采样是否为空） |

### CSV 导出格式（Java API 导出）
```
idx,className,methodName,time0,time1,childRefs
0,java.lang.Thread.run,,11516,48484,[20931]
...
```
- `idx`：节点池索引
- `time0/time1`：每个 time window 的采样时间（单位与 metadata.interval 一致）
- `childRefs`：子节点的池 index 列表（JSON 数组格式）

---

## 5. 资源重载触发

```java
// /eval 内触发 F3+T 等效的资源重载
minecraft.reloadResourcePacks();
```
配合 profiler：先 `runCommand("sparkc profiler start --timeout 90")`，再 `reloadResourcePacks()`。重载约 50-60s（大 mcpack + eyelib 解析）。

---

## 6. 验证 eyelib 接管渲染

确认负载实体确实走 eyelib 渲染路径（否则 profile 无意义）：
```java
// 查 RenderData 的 useBuiltInRenderSystem 标志
Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target);
java.lang.reflect.Field f = cap.getClass().getDeclaredField("useBuiltInRenderSystem");
f.setAccessible(true);
boolean ub = f.getBoolean(cap);
// ub=true → eyelib 接管渲染
```
