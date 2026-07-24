# 动画执行性能侦察报告

## 1. 每帧调用链（带 file:line）

### Layer 0: MC Render Tick 入口

```
FramePipeline.run()                              # src/main/java/io/github/tt432/eyelib/client/render/pipeline/FramePipeline.java
  ├── SetupStage.apply()             # EntityRenderOrchestrator.java:139-152  (模型/RC 装载)
  └── TickStage.apply()              # EntityRenderOrchestrator.java:158-211  (动画 tick)
        └── BrAnimator.tickAnimation()  # BrAnimator.java:18-44
```

### Layer 1: 动画 tick 调度

```
BrAnimator.tickAnimation()                              # BrAnimator.java:29-42
  ├── scope.getHostContext().put(HostRoles.MODEL_RUNTIME_DATA, infos)   # 78-79
  ├── component.getAnimate().entrySet().forEach                           # 35-41
  │     └── animation.tickAnimation()  → BrAnimationEntry.tickAnimation  # BrAnimationEntry.java:175
  │           └── BrClipExecutor.tick()                                   # BrClipExecutor.java:26-98
```

### Layer 2: 逐骨骼动画采样 (BrClipExecutor.tick 核心)

```
BrClipExecutor.tick()                                   # BrClipExecutor.java:26-98
  ├── 1x entry.blendWeight().eval(scope)                # 35   — MolangValue.eval
  ├── 1x entry.anim_time_update().eval(scope)           # 40   — MolangValue.eval
  ├── 1x data.owner.playbackState.tick()                # 41   — scalar ops
  ├── 1x 遍历 data.owner.effects() 处理音效/粒子/timeline    # 50-52
  └── entry.bones().int2ObjectEntrySet().forEach()       # 54-93 — **每骨骼迭代**
        └── 每骨骼:
              ├── infos.getData(boneName) → 读/写 ModelRuntimeData.Entry
              ├── boneAnim.lerpRotation(scope, animTick, 3x this*)  # BrBoneAnimation.java:74
              │     └── BrBoneAnimationSampler.sample()              # BrBoneAnimationSampler.java:23-28
              │           └── lerp() → 插值 (linear/catmullrom)     # BrBoneAnimationSampler.java:32-60
              │                 └── BrBoneKeyFrame.linearLerp()      # BrBoneKeyFrame.java:113-140
              │                       └── 3x MolangValue3.getX/Y/Z  # 每轴独立的 MolangValue.eval
              ├── boneAnim.lerpPosition(...)                        # 同上模式
              │     └── 同上...
              └── boneAnim.lerpScale(...)                         # 同上模式
                    └── 同上...
```

### Layer 3: 动画控制器 (状态机)

```
BrAnimationController.tickAnimation()　　                  # BrAnimationController.java:131-138
  └── BrControllerExecutor.tick()                          # BrControllerExecutor.java:30-62
        ├── switchState(): 遍历 states HashMap 查找                 # 86-131
        │     ├── currState.onExit().eval(scope)              # 97
        │     └── currState.onEntry().eval(scope)             # 108
        ├── 遍历 currState.transitions().entrySet() (每帧)       # 42-49
        │     └── 每 transition: entry.getValue().evalAsBool(scope)   # 43
        └── blend(): 当前状态 + 上一状态的混合                     # 133-165
              ├── currState.animations().forEach → 每个动画 tick
              └── lastState.animations().forEach → 混合期 tick
```

### Layer 4: Molang 求值 → 骨骼 pose 写入 (整个调用链)

```
MolangValue.eval(scope)                             # MolangValue.java:87
  └── method.apply(scope)                           # → MolangFunction / MolangCompiledFunction
        └── HiddenMolangExpression.evaluate(scope)  # MolangCompilerImpl.java:95-103
              └── Molang$Expr$<hash>.evaluate()     # MolangBytecodeEmitter.java — 字节码生成
                    ├── scope.get("this")           # 标识符 → ConcurrentHashMap.get
                    ├── scope.get("query.*") via identifier
                    │     └── resolveMemberAccess() # MolangRuntimeSupport.java:50-72
                    │           └── mappingTree.findField()     # MolangMappingTree.java:176-190 — HashMap lookup
                    └── resolveCall(scope, name, args)          # MolangRuntimeSupport.java:74-96
                          └── mappingTree.selectQueryVariant()  # 函数名+参数形状 → 变体选择
                                └── Class.getMethod/Method.invoke → MolangFunction

结果写入:
  renderInfoEntry.rotation.add(rotation)           # BrClipExecutor.java:69
  renderInfoEntry.position.add(position)            # BrClipExecutor.java:79
  renderInfoEntry.scale.mul(scale)                  # BrClipExecutor.java:89
```

---

## 2. 成本模型

### 2.1 三级嵌套循环

```
for each entity in level.entitiesForRendering():          # EntityRenderOrchestrator.java:160   O(E)
  for each Map.Entry<Animation, MolangValue> in animate:  # BrAnimator.java:35                   A
    for each bone in entry.bones():                       # BrClipExecutor.java:54               B
      for each axis of 3 channels (rot/pos/scale):        # BrClipExecutor.java:57-89            3C
        for each MolangValue3 axis (x/y/z):               # BrBoneKeyFrame.linearLerp            3
          scope.set("this", axisValue)                     #                                       1x ConcurrentHashMap.put
          MolangValue.eval(scope):                         #                                       1x $Expr$ bytecode
            → scope.get("this")                            #                                       1x ConcurrentHashMap.get
            → resolveMemberAccess("query.*") /             #                                       0-Nx mappingTree traversal
              resolveCall("query.*", args)                  #                                       0-Nx method invoke
```

**Total cost per frame**: `O(E × A × B × 3C × 3 × (MolangEval + MapOps))`

Where:
- E = visible entities (typical: 10-50)
- A = active animations per entity (typical: 1-5)
- B = bones per entity (typical: 20-80)
- 3C = 3 channels (rotation/position/scale)
- 3 = 3 axes per channel (x/y/z for `this`)

### 2.2 典型规模示例

| 参数 | 值 |
|---|---|
| 实体数 E | 20 |
| 每实体的动画数 A | 3 |
| 每动画的骨骼数 B | 40 (player 约 35-50, mob 约 20-30) |
| 通道数 C | 3 |
| 每通道轴数 | 3 |
| **总 MolangValue.eval 调用** | **20 × 3 × 40 × 3 × 3 = 21,600 次/帧** |
| **总 scope.set("this") 调用** | **21,600 次 ConcurrentHashMap.put** |
| **总 new Vector3f 分配** | **20 × 3 × 40 × 3 = 7,200 个/帧** |

### 2.3 动画控制器额外成本

```
for each entity with animation controllers:
  for each state transition entry:           # BrControllerExecutor.java:42-49
    1x MolangValue.evalAsBool(scope)         # 每 transition 一次求值
  for each animation in currState:          # 134-155
    1x blendValue.eval(scope) + 1x animation.tickAnimation
```

### 2.4 PartVisibility 额外成本

```
RenderControllerRuntime.evalPartVisibility()   # RenderControllerRuntime.java:20-30
  for each part_visibility entry:              # 每几何体
    for each MolangValue in condition list:    # 每个表达式
      molangValue.evalAsBool(scope)            # → MolangValue.eval
```

---

## 3. 分配热点清单

### 3.1 核心每帧分配

| # | file:line | 分配 | 每帧调用量 | 说明 |
|---|---|---|---|---|
| 1 | MolangValue3.java:70-77 | `new Vector3f(x,y,z)` | E×A×B×3C = ~7,200 | `evalWithThis` / `eval` 每骨骼每通道 |
| 2 | MolangScope.java:77-79 | `MolangFloat.valueOf(float)` 经 `set("this", v)` | E×A×B×3C×3 = ~21,600 | 每轴创建新 MolangFloat (除非 0/1) |
| 3 | BrBoneKeyFrame.java:124-138 | `MolangFloat.valueOf(value)` 在 `linearLerp` 内 | 同上, 每个 MolangValue.eval 内部 | Molang 字面量被缓存？非 0/1 仍 new |
| 4 | MolangRuntimeSupport.java:82 | `new ArrayList<>()` (visibleArgs) | E×A×B×3C×3 × query-count | 每个 resolveCall 创建 |
| 5 | MolangRuntimeSupport.java:83 | `new ArrayList<>()` (callShape) | 同上 | 每个 resolveCall 创建 |
| 6 | BrBoneKeyFrame.java:83-90 | `new Vector3f(...)` in `catmullromLerp` | 少 (仅 CATMULLROM) | 每骨骼每通道 |
| 7 | BrBoneKeyFrame.java:97-117 (setupCurvePoints) | `new ArrayList<Vector2f>()` + `new Vector2f(...)` | 同上 | CATMULLROM 每骨骼 4x Vector2f |
| 8 | BrClipExecutor.java:63-69 | `Vector3f rotation.mul(newMatrix).mul(...)` | E×A×B×3 | 3 通道×每骨骼 |
| 9 | BrClipExecutor.java:73-79 | 同上 position | E×A×B×3 | |
| 10 | BrClipExecutor.java:83-89 | 同上 scale | E×A×B×3 | |
| 11 | ModelRuntimeData.java:50 | `entries.computeIfAbsent(id, s -> new Entry())` | E×B 首次 | Entry 含 3x Vector3f new |
| 12 | BrAnimationEntry.java:171 (EFFECTS_CODEC) | TreeMap/ArrayList 序列化 | 仅加载时 | |
| 13 | MolangRuntimeSupport.java:55 | `WARNED_MISSING.add(methodName)` | 高频非路径 | HashSet write per missing query |

### 3.2 间接/非显式分配

| 来源 | 潜在分配 | 原因 |
|---|---|---|
| ConcurrentHashMap.get (scope.get) | Node[] 遍历 + hash 计算 | O(1) 但非平凡 |
| Molang$Expr$*.evaluate 字节码 | MolangFloat.valueOf 内联分配 | 每个字面量求值 new |
| resolveCall → Method.invoke | 反射参数数组 + boxing | 见 MolangRuntimeSupport.java:95-155 |
| ImmutableFloatTreeMap.floorEntry | TreeMap 寻路 | O(log n) |

---

## 4. 优化机会清单

排除已完成的 Opt1-6 (MolangMappingTree.findField 缓存、startsWith 优化、putAll 批量化、材质缓存、BrMaterialResolver 全局缓存、Molang 求值链分配消除)。

### Opt7: `scope.set("this", ...)` → 直接字段写入

- **file:line**: `MolangScope.java:77-79` (`set(String, float)`)
- **现状成本**: `ConcurrentHashMap.put + MolangFloat.valueOf(float)` — 每帧约 21,600 次调用，每次创建新 `MolangFloat` 对象（除非 0 或 1）
- **优化思路**: `MolangScope` 给 `"this"` 加专用 `float thisValue` 字段，取消 hashmap 插入。
  - `set("this", v)` → `this.thisValue = v;`
  - `get("this")` → `return MolangFloat.valueOf(thisValue)`
  - 或者更激进：生成字节码时，`BoundThisExpr` 直接读 `scope.getThisValue()` 返回 `float` 而非 `MolangObject`，消除包装
- **风险**:
  - Molang 内嵌 `this.xxx` 成员访问需要 `this` 作为 `MolangObject` 返回——保留包装路径但仅 `get("this")` 层
  - 必须同步 `MolangValue3.evalWithThis` 的逐轴设值语义（x → y → z 顺序）
  - 线程安全：当前每实体独立 scope，无线程竞争

### Opt8: `MolangValue3.evalWithThis` 合并三轴求值（批量化）

- **file:line**: `MolangValue3.java:57-63` (`evalWithThis`)
- **现状成本**: 三轴分别 `set("this", axVal) → getX(scope) → set("this", ayVal) → getY(scope) → set("this", azVal) → getZ(scope)`。每轴独立调用 MolangValue.eval，重复解析表达式 AST
- **优化思路**: 如果 `x/y/z` 三个 `MolangValue` 指向同一个 `MolangValue3`（且 lerp 模式 LINEAR），可合并为：
  - `scope.setAll("this", axVal, ayVal, azVal); // 设置三个轴值`
  - `MolangValue3.evalBulk(scope) // 一次遍历求三值`
  - 但 Molang 语法不支持 `this.x/this.y/this.z` 同时引用——`this` 只是 scalar。可行方案：将三轴的 MolangValue 合并为一个三元组表达式，通过 `molang(float thisX, float thisY, float thisZ) → Vec3` 内联字节码联合求值
- **风险**:
  - 当前设计明确区分逐轴 `this` 绑定（Mojang spec: "molang `this` scoped to the current axis value"）
  - 批量求值需证明语义等价：即 `f(x), g(y), h(z)` 与 `vec3(f(x), g(y), h(z))` 当 `this` 分别是 x/y/z 时等价
  - 若不同轴的表达式不同（常见，如 `rotation.z = query.anim_time * 30` 仅 z 轴有动画），合并无收益
  - 代码复杂度较高，可做 Opt7 之后再做

### Opt9: `linearLerp` 中 `scope.set("this")` 的冗余消除

- **file:line**: `BrBoneKeyFrame.java:124-138` (`linearLerp`)
- **现状成本**: 当前代码：
  ```
  scope.set("this", thisX); float ax = am3.getX(scope); float bx = bm3.getX(scope);  // set x
  scope.set("this", thisY); float ay = am3.getY(scope); float by = bm3.getY(scope);  // set y
  scope.set("this", thisZ); float az = am3.getZ(scope); float bz = bm3.getZ(scope);  // set z
  ```
  三轴每次都 `scope.set` + `getX`/`getY`/`getZ` 调用
- **优化思路**: 同 Opt7 + Opt8 的组合。若 `MolangValue3.getX/YZ` 能透传到 `MolangValue.eval` 时已绑定 `this`，消除 per-axis scope.set
- **风险**: 与 Opt7 重叠，需要在字节码生成层面支持 `evalWithThis` 语义

### Opt10: 空通道短路（无动画关键帧的骨骼跳过 lerp）

- **file:line**: `BrClipExecutor.java:57-89` + `BrBoneAnimation.java:74-82`
- **现状成本**: 每骨骼每帧调用 `lerpRotation`/`lerpPosition`/`lerpScale` 各一次，即使通道无关键帧。`BrBoneAnimationSampler.lerp` 内 `floorEntry/higherEntry` 返回 null → 最终返回 null，但之前已创建 `Vector3f` 并做了一轮 ImmutableFloatTreeMap 查找
- **优化思路**: `BrBoneAnimation` 预计算通道是否有活跃关键帧（`ImmutableFloatTreeMap.isEmpty()`）：
  ```java
  public boolean hasRotation = !rotation().isEmpty();
  public boolean hasPosition = !position().isEmpty();
  public boolean hasScale = !scale().isEmpty();
  ```
  在 `BrClipExecutor` 中：
  ```java
  if (boneAnim.hasRotation) {
      Vector3f rotation = boneAnim.lerpRotation(...);
      if (rotation != null) ...
  }
  // same for position, scale
  ```
- **风险**: 极小。`isEmpty()` 在构造时一次性计算。语义不变。

### Opt11: `ArrayList<Vector2f>` 消除 (CatmullRom setupCurvePoints)

- **file:line**: `BrBoneKeyFrame.java:97-117` (`setupCurvePoints`)
- **现状成本**: 每个 CATMULLROM 骨骼每通道创建一个 `new ArrayList<Vector2f>()` + 3-4 个 `new Vector2f(...)` + 每个 Vector2f 调用 MolangValue3.getX/YZ
- **优化思路**: 用固定大小数组 `float[] xs = new float[4]` + `float[] ys = new float[4]` 替代 ArrayList<Vector2f>：
  ```java
  float[] xs = new float[4];
  float[] ys = new float[4];
  int idx = 0;
  xs[idx] = before.timestamp();
  ys[idx++] = function.apply(...);
  // ...
  Curves.lerpSplineCurve(xs, ys, 4, weight / 3);  // 重载接受原始数组
  ```
- **风险**: 无语义风险。`Curves.lerpSplineCurve` 需加重载或原地修改。

### Opt12: 动画查找缓存 (AnimationLookup.get)

- **file:line**: `AnimationLookup.java:12` + `AnimationRegistries.java`
- **现状成本**: 每次 `tickAnimation` 内 `AnimationLookup.get(animName)` 调用委托给 `AnimationRegistries.animation().get(name)` → 通常是 `HashMap.get`。在 `BrControllerExecutor.blend` 中每动画每帧调用多次
- **优化思路**: `BrAnimationController.Data` 内缓存 `Map<String, Animation>`，首次查找后缓存。或 `AnimationComponent` 内 `animate` 的 key 已经是 `Animation` 对象（而非 String），消除了桶查找
- **风险**: 检查当前 `animate` 是 `Map<Animation, MolangValue>`，key 已直接是 Animation 对象。控制器路径 `animations.get(animName) → AnimationLookup.get(anim)` 仍经过字符串。可在 `BrControllerStateOwner` 加 `Map<String, Animation>` 缓存，`currentAnimations()` setter 时重建
- **备选**: 加 `@Nullable Animation` 字段 instead of 每次字符串查找

### Opt13: `MolangFloat.valueOf` 小常量池扩展

- **file:line**: `MolangFloat.java:5-10` (`valueOf(float)`)
- **现状成本**: 只有 0 和 1 的常量池命中。`scope.set("this", value)` 每次非 0/1 的浮点值都 new `MolangFloat`
- **优化思路**: 扩展常量池覆盖常见的值（如 0.5, 2, 180, 0.1, 0.333...）。但浮点池膨胀快——
  - 若 `this` 值是绑定姿势值（如 `bone.rotation.x()` 旋转），值分布极广
  - 更有效的是 Opt7（消除包装），而非扩展池
- **风险**: 浮点池的 `Float2ObjectOpenHashMap` 每次 `computeIfAbsent` 有开销且可能无限膨胀

### Opt14: `resolveCall` 的调用签名字符串分配消除

- **file:line**: `MolangRuntimeSupport.java:82-86` (`resolveCall`)
- **现状成本**: 每次 call 创建 `ArrayList<MolangObject> visibleArgs` + `ArrayList<VisibleArgumentKind> callShape`，再传入 `selectQueryVariant`
- **优化思路**: 若 `argValues` 长度已知（编译时已确定），可在 `MolangBytecodeEmitter` 生成字节码时直接构造 `VisibleArgumentKind[]` 数组，而非 ArrayList：
  ```
  // 生成代码而非运行时分配:
  VisibleArgumentKind[] shape = {NUMBER, NUMBER};
  ```
  但需兼容 varargs 动态参数数
- **风险**: 编译时参数数固定时才可行。varargs 路径需 fallback。

### Opt15: `MolangRuntimeSupport.resolveCall` 内 `Method.invoke` → `MethodHandle`

- **file:line**: `MolangRuntimeSupport.java:112-155` (`invokeMethod` → `Method.invoke`)
- **现状成本**: `method.invoke(null, args)` 每次反射调用，包含 `Object[]` 参数数组分配、类型检查和装箱
- **优化思路**: `MolangFunction` 注解处理器在初始化时生成 `MethodHandle` 并缓存，运行时 `MethodHandle.invoke` 代替 `Method.invoke`：
  ```java
  // MolangMappingTree.FunctionInfo 加: @Nullable MethodHandle mh
  public MethodHandle methodHandle() { /* lazily create via lookup.unreflect */ }
  ```
- **风险**:
  - `Method.invoke` 本身已在 JIT 中有优化（inflation threshold 15），非严重热点（相比 scope.set 等）
  - `MethodHandle.invoke` 仍需要 `Object[]` 数组传递实参（除非用 `invokeExact` 但类型签名固定不易）
  - 收益中等，优先 Opt7/Opt10

### Opt16: 动画控制器 `transition` 短路——不变条件跳过重求值

- **file:line**: `BrControllerExecutor.java:42-49`
- **现状成本**: 每帧遍历所有 `transitions` 的 `entrySet()`，对每个 transition 的 `MolangValue` 调用 `evalAsBool(scope)`。当状态未变时（绝大多数帧），这些求值完全冗余
- **优化思路**: 如果 `transition` 的 MolangValue 表达式不包含 `variable.*` 等每帧变化值（编译时标记为 "static"），缓存结果。更简单：引入状态驻留帧数计数，仅在状态变化后 N 帧内验证 transition（设 N=0 即禁用缓存）
- **风险**:
  - 表达式可能依赖 `query.anim_time`、`variable.*` 等每帧变化量，缓存失效
  - 安全实现：只有经编译器标记为 "tick-independent" 的纯表达式才缓存
  - 也可在数据层面支持——Molang 表达式提供 `hasSideEffects()` 方法判断

---

## 5. 优化优先级建议

| 优先级 | Opt | 预期收益 | 代码量 | 风险 |
|---|---|---|---|---|
| P0 | Opt7 (`this` 字段化) | **极高**: ~21,600 次/帧 ConcurrentHashMap.put + MolangFloat new 消除 | 小 | 中（需验证语义等价） |
| P0 | Opt10 (空通道短路) | **高**: 静默骨骼通道跳过 ~50-70% 调用量 | 极小 | 低 |
| P1 | Opt11 (CatmullRom ArrayList 消除) | **中**: CATMULLROM 帧减少 4x 对象分配 | 小 | 低 |
| P1 | Opt12 (控制器内 Animation 缓存) | **中**: 减少 HashMap.get 字符串查找 | 小 | 低 |
| P2 | Opt8 (三轴合并) | **高但复杂**: 三轴联合求值减少 2/3 set+get | 大 | 高（Molang this 语义约束） |
| P2 | Opt14 (resolveCall shape 数组) | **低**: 减少 ArrayList 分配 | 小 | 中 |
| P3 | Opt15 (MethodHandle) | **低**: JIT 已优化反射 | 中 | 低 |
| P3 | Opt16 (transition 缓存) | **中**: 少数场景 | 中 | 中（语义失效风险） |
