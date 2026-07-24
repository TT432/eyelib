# Eyelib 渲染 live-set 泄漏定位结果

## 结论

已定位并修复一个确定的 Java heap retention：`RenderHelper` 的实例初始化器在每次 `RenderHelper.start()` 创建 helper 时，向全局 Forge/NeoForge EventBus 注册一个 `ManagerEntryChangedEventPublisher` listener。EventBus 以静态强引用保存 listener，导致每帧、每模型组件持续累积监听器及其包装容器。

责任点：

- `src/main/java/io/github/tt432/eyelib/client/render/RenderHelper.java`
- `RenderHelper.start()` 每次返回 `new RenderHelper()`。
- 原实例初始化器 `{ ManagerEntryChangedEventPublisher.addListener(...) }` 因此每个 helper 执行一次。
- 修复为 `static { ... }`，保留 DFS model cache 失效行为，但只注册一次。

## 复现证据

诊断场景：1.20.1 fresh JVM，固定 `WORLD stability n64`，warmup 30 秒、measure 600 秒；JFR profile + `path-to-gc-roots=true`。在 measurement 起点、中段、终点执行 `jcmd GC.class_histogram`，每次触发 full GC，因此以下数字是 retained live objects，不是普通年轻代分配。

### Full-GC live set

| 采样点 | live instances | live bytes |
|---|---:|---:|
| t0 | 19,820,122 | 971,461,720 |
| t1 | 43,564,921 | 1,693,090,240 |
| t2 | 53,275,716 | 1,919,567,344 |

`t0 → t2` retained bytes 增长 948,105,624 bytes（约 904.18 MiB）。

### 主导增长类

| 类 | t0 instances | t2 instances | 增量 |
|---|---:|---:|---:|
| `ManagerEntryChangedEventPublisher$$Lambda...` | 536,321 | 5,314,689 | 4,778,368 |
| `EventBus$$Lambda...4697` | 536,360 | 5,314,728 | 4,778,368 |
| `EventBus$$Lambda...4698` | 536,360 | 5,314,728 | 4,778,368 |
| `Collections$SynchronizedRandomAccessList` | 536,887 | 5,315,254 | 4,778,367 |
| `ConcurrentHashMap$Node` | 645,442 | 5,428,035 | 4,782,593 |

测量期 39,817 帧，对应约 120.01 个新 listener/帧、7,964 listener/秒；与 64 实体的多个 ModelComponent render call 数量级一致。

## GC root 与分配证据

JFR `OldObjectSample` 捕获到 9 个 EventBus 相关 retained sample。典型 GC root chain：

```text
Object[] elementData
→ ArrayList
→ Collections$SynchronizedRandomAccessList
→ ConcurrentHashMap$Node
→ ConcurrentHashMap listeners
→ net.minecraftforge.eventbus.EventBus
→ static EVENT_BUS
→ Class
```

典型分配栈：

```text
ArrayList.grow/add
→ Collections$SynchronizedCollection.add
```

以及：

```text
ConcurrentHashMap.computeIfAbsent
→ EventBus.addToListeners
→ EventBus.addListener
```

Histogram 中实际 retained listener 类型是：

```text
io.github.tt432.eyelib.bridge.event.ManagerEntryChangedEventPublisher$$Lambda...
```

这与 `RenderHelper` 实例初始化器调用 `ManagerEntryChangedEventPublisher.addListener` 的源码路径闭合。

## 修复验证

修复后使用另一个 fresh JVM，固定 64 实体运行 120 秒；在 measurement 起点和终点再次执行 live histogram：

| 指标 | 起点 | 终点 | 变化 |
|---|---:|---:|---:|
| full-GC live bytes | 811,068,392 | 794,709,984 | -15.60 MiB |
| full-GC live instances | — | — | -38,420 |
| `ManagerEntryChangedEventPublisher$$Lambda` instances | 2 | 2 | 0 |
| 所有 `EventBus$$Lambda` instances | 88 | 88 | 0 |

因此：

- EventBus listener 线性增长已停止。
- 120 秒后 full-GC live set 未增长。
- 原 DFS model cache 的 manager-entry 失效 listener 仍存在，实例数固定。

## 排除项

源码和 histogram 均不支持以下对象是本次主泄漏：

- `FramePlan`、`ModelVisitContext`：每帧/每 render call 局部对象，未进入主导 retained classes。
- `ModelComponent`：每帧重建造成高 allocation pressure，但 full-GC histogram 的主导 retained 增长不是该类。
- `RenderData`/实体 attachment：固定 64 实体且未出现持续实体/RenderData live-count 增长。
- `RenderHelper.dfsModels`、two-side baked cache、AnimationComponent controller data：key 空间由固定资源定义限定；本次 live histogram 不显示其线性增长。

## 原始产物

目录：`work/render-benchmark-leak-investigation/`

- `eyelib-stability-final.jfr`：734 秒、约 77 MiB。
- `histogram-t0/t1/t2.txt`：修复前三次 full-GC live histogram。
- `histogram-fix-t0/t1.txt`：修复后两次 full-GC live histogram。
- `heap-*.txt`：对应采样点 heap info。
- `old-object-summaries.json`：223 个 OldObjectSample 的压缩摘要。

注意：诊断 run 使用 JFR profile，并执行 full GC，不能把该 run 的 FPS 与无 JFR baseline 比较。
