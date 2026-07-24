# 动画执行与渲染性能优化计划（ms/frame）

> 状态：计划（未实施）。输入：两份代码级侦察报告（附录 A/B）+ 既有 spark 基线（docs/perf/spark-baseline-and-optimizations.md，Opt1–6 已合入）+ clientBenchmark 基线（run-20260724-231836）。
> 本文只定义优化的决策变量、目标函数、约束与子任务规格；实施时每个子任务独立验证、独立提交。

## 1. 基线数据

### 1.1 clientBenchmark（1.20.1，mixed n96 = slime/zombie/skeleton/cow 各 24）

| 场景 | render_work P50 | render_work P99 | 帧间隔 P99.9 | 来源 |
|---|---:|---:|---:|---|
| fbo-mixed-n96 | 8.57 ms | 11.42 ms | 14.62 ms | run-20260724-231836 |
| world-mixed-n96 | 46.73 ms | 62.43 ms | 80.85 ms | 同上 |

FBO 场景隔离了 eyelib 实体渲染成本；world 场景的 46.7ms 包含世界渲染 + GL 驱动等待，eyelib 占比需 spark 归因，不能直接当作 eyelib 成本。

### 1.2 spark（Opt6 后，60s 稳态渲染，83 实体）

- eyelib self 总量 2964ms / 83 实体 ≈ **36 μs/实体/帧**（采样窗口归一化），较基线 -71%。
- 残留 eyelib 热点：`Molang$Expr$*.evaluate` ~588ms self（字节码表达式求值）。
- TOP self 已转为非 eyelib：GL 驱动、LinkedHashMap.forEach、Int2ObjectOpenHashMap、HashMap.putVal。

### 1.3 成本模型（代码结构推得，已抽验）

```
帧成本 = E × [ 动画tick + Σ_components ( resolveOutput + 骨骼遍历 + 顶点变换 + 顶点写入 ) ] + flush/draw
动画tick = A × ( 2×entry级eval + B × 3通道 × ( TreeMap查找×2 + 3轴 × (scope.set("this") + Expr.evaluate) ) )
顶点变换 = B × V × ( ~15 FMA transformPos + ~22 FMA + 1 sqrt transformNormal )
```

E=实体，A=活跃动画，B=骨骼，V=顶点/骨骼。96 实体规模下分配量级（动画侧）：~21,600 次 `scope.set("this")`（ConcurrentHashMap.put + 非常量 `new MolangFloat`）、~7,200 个 `new Vector3f`/帧（MolangValue3.eval/evalWithThis）。

## 2. 优化三要素

### 2.1 目标函数

- 主目标：`world-mixed-n96` render_work P50 与 P99（ms/frame）下降。
- 副目标：帧间隔 P99/P99.9（hitch）下降；每帧堆分配字节数下降（GC pause via resources.csv）。
- 护栏：FBO 场景吞吐不得回退；26.1.2 同版本回归通过。

### 2.2 决策变量

按 ROI 排序的子任务（§3、§4），每个可独立开关、独立测量。

### 2.3 约束条件

- **语义不变量**：molang `this` 逐轴绑定语义（Mojang syntax-guide：`this` = 表达式写入目标的当前值）；Bedrock 每帧 bind pose 重置 + 逐通道累加模型（ADR-0013）；材质 passOrder 排序（不透明先、半透明后）。
- **跨版本**：1.20.1 / 1.21.1 / 26.1.2 三节点行为对齐（RenderSink 兼容层）；`//?` 条件注释规范。
- **架构**：bridge/domain 分层（ADR-0010/0016），优化不得让 domain 依赖 MC 类。
- **测量纪律**：benchmark 期间禁 spark/RenderDoc/截图/`/eval`；fresh JVM、baseline/candidate 交错；不用 best-of-N。

## 3. 动画执行子任务

### A1 · `this` 绑定字段化（P0，预期收益最高）

- **位置**：`MolangScope.java`（set/get "this"）、`MolangValue3.java:76-86`（evalWithThis）、`MolangBytecodeEmitter`（this 读取指令）。
- **现状**：每轴求值前 `scope.set("this", v)` → `MolangFloat.valueOf`（非 0/1 即 new）→ `ConcurrentHashMap.put`；求值时 `cache.get("this")` + parent 链检查。96 实体 ≈ 21,600 次 put + 21,600 次 get/帧。
- **方案**：MolangScope 增加 `float thisValue` 专用字段 + `setThis(float)`/`thisValue()`；字节码后端读 `this` 时生成 `MolangRuntimeSupport.thisValue(scope)` 直接返回 float，绕过 Map 与装箱。Map 路径保留给非 float 的 `this` 绑定（若存在）。
- **前置条件**：全仓 grep 确认 `set("this"` 的所有写入点均为 float 语义；确认无 `this.xxx` 成员访问用法（`this` 在关键帧语境是标量）。
- **后置条件**：Expr.evaluate 内不再出现 "this" 的 Map 读写；分配计数中 MolangFloat 显著下降。
- **不变量**：逐轴绑定顺序 x→y→z；嵌套表达式内 `this` 读到当前轴值；异常路径（eval 抛错）不残留错误 this（当前语义同样残留，保持一致即可）。
- **副作用/风险**：若有脚本/实体语境把 `this` 绑为实体对象（Bedrock `q.this`？），字段化会丢语义——抽验时未发现此类用法，实施前必须 grep 验证。
- **验证**：spark 60s 对比（MolangFloat 分配、Expr.evaluate self）；mixed n96 benchmark。

### A2 · 空通道短路（P0，风险最低）

- **位置**：`BrClipExecutor.java:54-93`、`BrBoneAnimation.java`（sample/lerp*）。
- **现状**：无关键帧的通道（position/scale 常为空）仍执行：12 行 `this*` 计算（含 bind 读取、乘法）+ `BrBoneAnimationSampler.sample` → 2 次 `ImmutableFloatTreeMap` 查找 → 返回 null。空通道在白名单骨骼上占比通常 ≥50%。
- **方案**：`BrBoneAnimationDefinition`/`BrAnimationChannel` 暴露 `hasChannel(name)`（构造期固化）；BrClipExecutor 每骨骼先查三个布尔，空通道连 `this*` 计算一起跳过。
- **前置/后置**：通道有无关键帧在加载后不变（动画资源不可变）；短路后 renderInfoEntry 不变 == 现状 null 路径。
- **不变量**：非空通道行为逐位不变；`this` 语义不受影响（空通道本就不求值）。
- **风险**：极低。
- **验证**：单测（空通道骨骼 tick 后 Entry 为零向量）；benchmark。

### A3 · resolveCall 参数形状数组化（P2）

- **位置**：`MolangRuntimeSupport.java:82-86`（每次 call new 2×ArrayList）。
- **方案**：固定参数数的调用在字节码生成期构造 `VisibleArgumentKind[]` 常量，varargs 路径保留 ArrayList。
- **风险**：低；收益中等（仅非纯量表达式多的包明显）。

### A4 · CatmullRom 采样分配消除（P1）

- **位置**：`BrBoneKeyFrame.java` setupCurvePoints（每骨骼每通道 new ArrayList + 3–4×Vector2f）。
- **方案**：改 `float[4] xs/ys` 原始数组，`Curves.lerpSplineCurve` 加原始数组重载。
- **风险**：低。

### A5 · 控制器 Animation 查找缓存（P1）

- **位置**：`BrControllerExecutor` blend/animations 迭代中 `AnimationLookup.get(String)`。
- **方案**：状态切换时一次性解析为 `Animation` 引用数组缓存，驻留期间直接迭代。
- **风险**：低；资源重载时需失效（挂到 ManagerReplacedEvent/状态重建点）。

### A6 · transition 求值短路（P3，暂缓）

- 依赖编译期"tick-independent"标记，语义失效风险大于收益。列入但不排期。

## 4. 渲染子任务

### R1 · 1.20.1/1.21.1 跨实体合批（P0，渲染侧预期收益最高）

- **位置**：`EntityRenderOrchestrator.renderComponents`（每实体 `sink.flush()`）+ `ImmediateRenderSink.flush`（→ endBatch）。
- **现状**：每实体 endBatch → ~2 draw call/实体，96 实体 ≈ 166–192 draw/帧。26.1.2 DeferredRenderSink 已由 vanilla renderAllFeatures 按 RenderType 合批，1.20.1 路径没有。
- **方案**：实体渲染循环内不 flush；同一 RenderType 的多个实体写共享 VertexConsumer（MultiBufferSource 本就同类型同 buffer），帧末统一 endBatch。透明 pass 保持 passOrder 全局排序后提交。
- **前置条件**：确认 1.20.1 下 eyelib 实体渲染与 vanilla 实体渲染的交错顺序不被破坏（vanilla 每实体自带 bufferSource.endBatch？需核实 LevelRenderer 实体段落的 flush 语义）。
- **不变量**：passOrder（SOLID/ALPHA_TEST 先于 TRANSLUCENT/ADDITIVE）；同材质实体间绘制先后不影响正确性（深度测试兜底，透明实体 BE 规范本就无序）。
- **风险**：中。渲染顺序变化可能暴露既有深度/混合 bug；用 FBO 像素对比（现有 clientsmoke 场景）做回归。
- **验证**：benchmark fbo/world mixed n96；draw call 计数（RenderDoc 或 GL 计数器）166 → 预期 <20。

### R2 · renderComponents 每实体分配消除（P1）

- **位置**：`EntityRenderOrchestrator.java` renderComponents：`new ArrayList + sort + stream`（每实体）、`new AnimationEffects()` fallback、`collectBindBones` 每 tick `new Int2ObjectOpenHashMap`。
- **方案**：components 列表加版本号/脏标记缓存排序结果（或直接在 component 增删点排序）；stream→for；AnimationEffects 用共享 EMPTY 常量或惰性分配；collectBindBones 结果按 cap 缓存失效。
- **风险**：低。注意 passOrder 排序必须在材质缓存失效（Opt4 identity 变化）后重排。

### R3 · 26.1.2 ByteBufferBuilder 复用（P1，仅 26.1.2）

- **位置**：`EyelibLivingEntityRenderer.submit`（每实体 `new ByteBufferBuilder(786432)` + `immediate()` + close，768KB 堆外/实体/帧）。
- **方案**：ThreadLocal 复用 + 帧末 reset；或改走 DeferredRenderSink 的 submitCustomGeometry（不再自建 bufferSource，与 vanilla 合并路径一致）——后者同时解决 R1 类问题，优先评估。
- **风险**：高（buffer 生命周期）；替代方案（去自建 buffer）风险反而低，先验证可行性。

### R4 · Attachable 路径对齐（P2）

- attachable 与主路径共享 RenderHelper/DFSModel，但无 flush、无 passOrder 排序。随 R1/R2 顺带对齐，避免双份优化。

### 搁置项（ROI 不足或架构不符）

| 项 | 理由 |
|---|---|
| BakedBone.transformPos/Normal 手动向量化（Panama） | JIT 自动向量化未证伪；Java 17 Vector API 是 incubator |
| GPU skinning / 每骨骼 uniform | immediate-mode VertexConsumer 架构不支持，需整体重写提交路径 |
| part_visibility 求值缓存 | 依赖追踪成本 > eval 本身；BE 规范逐帧求值 |
| MolangFloat 常量池扩展 | 值分布广，池膨胀；A1 直接消除包装更优 |
| MethodHandle 替换 Method.invoke | JIT inflation 后反射非热点 |

## 5. 已知正确性隐患（优化前需裁决，非本计划范围）

- **DFSModel 线性化疑似打断父子骨骼变换继承**（侦察发现，未证实）：PreBoneFrame/PostBoneFrame 平铺后，子骨骼 pushPose 时父骨骼变换已被 pop。A&S 多为扁平骨骼结构可能掩盖问题。已登记 feedback，需独立正确性任务验证（含 attachable 路径）。

## 6. 测量与验收

1. 每个子任务实施前后跑：`runClientBenchmark -Deyelib.benchmark.modes=mixed`（1.20.1 必跑，26.1.2 涉及 R3 时跑）。
2. 动画侧任务加跑 spark 60s 稳态（配方：docs/perf/spark-profiling-recipe.md），对比 Expr.evaluate self 与分配。
3. 验收阈值（建议）：A1+A2 合入后 fbo-mixed-n96 render_work P50 下降 ≥10%；R1 合入后 world-mixed-n96 render_work P50 下降可测且 FBO 无回退。
4. 语义回归：`:1.20.1:test` + clientsmoke 相关场景全绿。

## 7. 建议执行顺序

1. A2（空通道短路）— 最小改动立基线。
2. A1（this 字段化）— 最大动画收益，需先做 `this` 用法 grep 验证。
3. R2（分配消除）— 低风险铺垫。
4. R1（1.20.1 合批）— 最大渲染收益，需 FBO 像素回归。
5. R3（26.1.2 buffer）— 与 R1 同期评估。
6. A3/A4/A5 — 按 profile 结果裁剪。

## 附录

- 附录 A：动画链路侦察报告（anim-scout-report.md）
- 附录 B：渲染链路侦察报告（render-scout-report.md）
