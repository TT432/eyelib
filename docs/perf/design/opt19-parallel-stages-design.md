# Opt19：SetupStage/TickStage 并行化——线程模型设计

日期：2026-08-29。状态：设计已评审（实证先行），实现中。

## 目标

Render 线程深栈 JFR（opt18-after2，--stack-depth 64）：SetupStage 37.7% + TickStage 38.9% ≈ 77% 的渲染线程样本。
两阶段的逐实体工作（molang 求值、组件重建、动画采样）跨实体天然独立，目标是在**不改任何可观察语义**（逐帧求值、顺序、粒子时机同帧）的前提下跨实体并行。

## 执行模型

- 专用 ForkJoinPool `eyelib-render-workers`，大小 `clamp(2, 8, availableProcessors/2)`，
  开关 `-Deyelib.parallelStages=true|false`（默认 true）、`-Deyelib.parallelStages.threads=N`、
  `-Deyelib.parallelStages.minEntities=8`（低于阈值串行，避免小场景开销）。
- 渲染线程 `pool.invoke(task)` 阻塞 join——submit/join 建立 happens-before：
  tick 期（客户端线程）对实体状态的写入 → worker 读可见；worker 对 per-entity cap 的写入 → join 后渲染线程可见。
- 分片：实体列表按连续索引区间切成 2×workers 份；每片私有结果列表；join 后按索引序拼接——
  **结果顺序与串行逐实体迭代完全一致**（setupResults/deferredEffects/tickResults 顺序保持）。
- 异常：worker 内捕获首个异常，join 后在渲染线程重抛（保持现有 recordError+上抛语义）。

## 共享可变状态清单（实证核查结果）

| 状态 | 位置 | 判定 | 处置 |
|---|---|---|---|
| MemberSite (tree/epoch/minimal/full) | 生成的表达式实例字段，`compileCache` 全局共享（按表达式字符串） | **普通字段，并发发布会撕裂** | 改为 `volatile Binding` 不可变快照（单次读快照→判定→用快照内绑定；重建=新快照+CAS 语义 volatile 写） |
| ZERO_ARG_CACHE | CHM + 不可变 ZeroArgCacheEntry | 已安全 | 不动 |
| METHOD_HANDLES / FIELD_GETTERS / SPREAD_INVOKERS | CHM computeIfAbsent | 已安全 | 不动 |
| WARNED_MISSING | synchronizedSet | 已安全 | 不动 |
| HostRole REGISTRY / NEXT_ID | CHM + AtomicInteger | 已安全 | 不动 |
| MolangMappingTree | 纪元守卫，仅 reload 期突变 | 帧内只读，安全 | 不动 |
| GlobalBoneIdHandler map/map2/counter | 裸 fastutil + computeIfAbsent | **并发插入可损坏** | 两个 get 方法 static synchronized（稳态仅查找，成本可忽略） |
| MOLANG_VALUE_CONSTANT_POOL | Float2ObjectOpenHashMap | 非线程安全 | 换 ConcurrentHashMap |
| AttachableItemRenderSetup.CACHE | static WeakHashMap | **tickForEntity 逐实体写** |  hoist 出并行段：TickStage join 后渲染线程按实体序串行执行 |
| ParticleSpawnApi（spawn/updatePose/remove） | render 线程粒子管理器 | **tickAnimation 求值内即时调用** | DeferredAnimationParticleSpawner：worker 期间入队（逐实体队列），join 后按实体序 drain 到真实 adapter |
| syncedActions（NativeImagePort.download=GPU 读回） | Setup 期已 deferred；Tick stale 路径 inline | GPU 操作必须渲染线程 | Tick 路径改为逐实体收集，join 后按序 drain |
| flushOrphaned → spawner.remove | 同上 | 收集到逐实体队列，drain 时执行 |
| plan 三个结果列表 | ArrayList | 单线程假设 | 分片私有列表 + 索引序拼接 |
| frameCounter | 渲染线程帧序号 | 并行在同一帧内 | 不动 |

## 逐实体私有状态（天然安全）

RenderData/MolangScope（singleThreaded）/AnimationComponent/ModelRuntimeData 数组/RC Slot 值键缓存/
ModelComponent 列表/bindBones 懒缓存/控制器 resolveAnimation 缓存——均逐实体所有，
每实体在任一片内仅由一个 worker 处理 → 等价单线程。

## 外部读

实体字段读：渲染相内客户端线程阻塞在 join，无并发写；HB 由 FJ submit 保证。
level 查询读：帧内只读，有界风险（minecraft 代码非线程安全但此处无并发突变）。
注册表读（ClientEntityManager/RenderControllerManager/AnimationRegistries）：帧内只读，代际失效不变。

## 语义保持论证

1. molang 逐帧求值：不缓存求值结果——并行只改变"哪个线程算"，不改变"算什么、算什么顺序写入实体私有状态"。
2. 粒子时机：spawn/updatePose/remove 从"求值中即时"变为"TickStage 末尾同帧 drain"——粒子在实体之后的粒子相渲染，同帧内不可观察。
3. 顺序：结果列表/效果/粒子全部按实体索引序拼接/drain，与串行一致。
4. MemberSite 快照竞争：读侧只使用单一快照内的 (tree, epoch, minimal, full) 四元组——要么旧纪元一致绑定（下次调用重解析，与现注释承诺的"良性竞争"等价但一致），要么新纪元一致绑定；不会撕裂。
5. setupClientEntity 内 GlobalBoneIdHandler 只查不插（骨骼 id 在资源解码期经 STRING_ID_CODEC 全部插入）——synchronized 兜底覆盖首帧/边界。
6. AttachableItemRenderSetup.tickForEntity 不消费 tickAnimation 产物（读实体装备+自建 item RenderData），hoist 到 join 后同帧串行，顺序保持。

## 开关语义

- `parallelStages=false` 或实体数 < minEntities：完全走现有串行代码路径（零行为变化）。
- 统一改动（MemberSite 快照、GlobalBoneIdHandler synchronized、常量池 CHM、DeferredSpawner 语义）
  在串行模式下同样生效——它们是发布安全性修复，不改变串行结果。

## 契约测试（单元，无 MC 运行时）

1. MemberSiteConcurrentResolutionTest：N 线程锤 resolve + 中途换树（epoch++），断言全部结果 == 串行基线、无异常。
2. ParallelStageExecutorTest：分片覆盖（每索引恰好一次）、合并序 == 索引序、异常传播、阈值串行回退、空列表。
3. DeferredAnimationParticleSpawnerTest：操作顺序保持、drain 精确回放、spawn 返回 true。
4. GlobalBoneIdHandlerConcurrentTest：并发同名同 id、异名异 id、get(int) 反查一致。
5. 常量池并发：多线程 getConstant 同值同实例。

## 验证

- 三版本编译 + 1.20.1/1.21.1 全量单测。
- 运行时：串行 vs 并行截图对比（姿态一致）、反射探针（pool 大小/worker 数）、JFR --stack-depth 64 复测。
- 基准：`:1.20.1:runClientBenchmark` / `:1.21.1:runClientBenchmark` world n384 slime 45s 串行协议，
  -Deyelib.parallelStages=false/true A/B。

## 已知边界（不做）

- renderComponents（GL 提交）保持渲染线程串行——GL 上下文单线程约束。
- Setup 期 deferredEffects → EffectCommitStage 机制不变（已在并行段外）。
- 服务器线程并发：worker 数上限 8 且渲染相服务器 tick 通常已结束；不抢占全部核。
