# Render Pipeline — 性能侦察报告

## 1. 每帧渲染调用链（file:line）

### 1.1 版本入口

#### 1.20.1 / 1.21.1

```
MC LevelRenderer.renderLevel
  → RenderLevelStageEvent (AFTER_SKY)
    → RenderStageEventAdapter.onRenderLevelStage()
       bridge/client/render/adapter/RenderStageEventAdapter.java:43-56
    → @OnRenderStage dispatch (FramePipeline)
  → LivingEntityRenderer.render()
    → EyelibLivingEntityRenderer.render()
       bridge/client/render/EyelibLivingEntityRenderer.java:43-80  (<26.1)
    → RenderPorts.get().renderEntityPort().render(RenderEntityParams)
       → EntityRenderOrchestrator.renderEntityFromParams()
          client/render/EntityRenderOrchestrator.java:159-174
```

Also mass-iterates all entities in `renderEntities()` at EntityRenderOrchestrator.java:119-157, called from `RenderPorts.renderBufferPort()`.

#### 26.1.2

```
MC LevelRenderer.renderLevel
  → RenderLevelStageEvent.AfterOpaqueBlocks
    → RenderStageEventAdapter.onRenderLevelStage()
       bridge/client/render/adapter/RenderStageEventAdapter.java:58-71
    → @OnRenderStage dispatch (FramePipeline) — 不再在此批量渲染实体
  → LivingEntityRenderer.submit()
    → EyelibLivingEntityRenderer.submit()
       bridge/client/render/EyelibLivingEntityRenderer.java:110-134  (>=26.1)
    → ByteBufferBuilder(786432) + MultiBufferSource.immediate()  ← 每实体分配
       EyelibLivingEntityRenderer.java:120-121
    → RenderPorts.get().renderEntityPort().render(RenderEntityParams)
```

### 1.2 编排层 (EntityRenderOrchestrator)

```
EntityRenderOrchestrator.renderEntity()
  client/render/EntityRenderOrchestrator.java:228-255
  → renderComponents(data)
     client/render/EntityRenderOrchestrator.java:271-350

renderComponents():
  274: ArrayList<>(modelComponents) + sort(Comparator.comparingInt(passOrder))  ← 每实体分配
  277: components.stream().filter(...).mapToLong(...).sum() > 0  ← stream分配
  291: resolveOutput() → ModelComponent.getRenderType() → MaterialPort.toRenderType()
  293: data.sink().submit(renderPass, texture, poseStack, GeometryWriter lambda)
  297: sink.flush() (ImmediateRenderSink → endBatch)
```

### 1.3 RenderSink (版本差异 Port)

**ImmediateRenderSink (<26.1):**
```
submit() → MaterialPort.toRenderType(renderPass, texture) → bufferSource.getBuffer()
         → writer.write(pose.last(), consumer)
bridge/client/render/ImmediateRenderSink.java:30-42
flush() → bufferSource.endBatch()
ImmediateRenderSink.java:44-47
```

**DeferredRenderSink (>=26.1):**
```
submit() → MaterialPort.toRenderType(renderPass, texture)
         → collector.submitCustomGeometry(pose, renderType, writer)
bridge/client/render/DeferredRenderSink.java:32-44
flush() → 空操作 (由 renderAllFeatures 统一绘制)
```

### 1.4 模型遍历与顶点写入

```
RenderHelper.render()
  client/render/RenderHelper.java:62-68
  → ModelBakePort.twoSideGetBakedModel(model, isSolid, texture, meshTexture)  ← 缓存命中
  → DFSModel.visit(params, context, RENDER_VISITOR, infos, stateMachine)

DFSModel.visit()
  client/model/DFSModel.java:31-34
  → frames.forEach(frame → frame.visit(...))

RENDER_VISITOR = HighSpeedRenderModelVisitor
  client/render/visitor/ActiveModelRenderVisitors.java:12

DFSModel 帧序列 (per bone):
  PreModelFrame → visitPreModel() → poseStack.pushPose() + rotateY(180°)
                  ModelVisitor.java:95-102
  PreBoneFrame  → HighSpeedRenderModelVisitor.visitPreBone()
                  visitor/HighSpeedRenderModelVisitor.java:22-41
                  → poseStack.pushPose()
                  → applyBoneTranslate(context, poseStack, bone, data)
                    ModelVisitor.java:125-133  → 变换骨骼姿态 + 缓存复制
                  → renderBakedBone(renderParams, bakedBone)
                    HighSpeedRenderModelVisitor.java:43-52
  PostBoneFrame → visitPostBone() → poseStack.popPose()
  PostModelFrame → visitPostModel() → poseStack.popPose()

renderBakedBone():
  48: bakedBone.transformPos(last.pose())
      bridge/client/render/bake/BakedModel.java:90-102
  49: bakedBone.transformNormal(last.normal())
      bridge/client/render/bake/BakedModel.java:104-122
  59-77: visitVertex() 循环 → per vertex:
    VertexConsumerPort.vertex(consumer, pos.xyz, r,g,b,a, u,v, overlay, light, n.xyz)
    bridge/client/render/VertexConsumerPort.java:11-27

VertexConsumerPort.vertex():
  <1.20.6: consumer.vertex(x,y,z, r,g,b,a, u,v, overlay, light, nx,ny,nz)
  >=1.20.6: consumer.addVertex(x,y,z).setColor(r*255).setUv(u,v).setOverlay(overlay).setLight(light).setNormal(nx,ny,nz)
```

### 1.5 材质→RenderType 映射链

```
ModelComponent.getRenderType(texture)
  capability/component/ModelComponent.java:119-132
  → currentMaterialMap() (返回 MaterialManager.INSTANCE.all() snapshot)
  → resolveEntry(matMap, path)  ← 每 component 缓存 (Opt4)
  → resolveCachedMaterial(entry, matMap)  ← 每 component 缓存 (Opt4)
  → RenderTypeResolver.resolve(texture, material)
    → BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(material))
      bridge/material/adapter/BrRenderTypeFactory.java:47-66

BrRenderTypeFactory.create():
  CACHE.computeIfAbsent(new Key(texture, state), key→custom(tex,state))
  bridge/material/adapter/BrRenderTypeFactory.java:64

custom() <26.1:  # BrRenderTypeFactory.java:108-135
  RenderType.create(name, NEW_ENTITY, QUADS, SMALL_BUFFER_SIZE, false, transparent,
                    CompositeState.builder().setShaderState(...).setTextureState(...)...)

custom() >=26.1:  # BrRenderTypeFactory.java:181-208
  PIPELINE_CACHE.computeIfAbsent(state, this::buildPipeline)
  RenderSetup.builder(pipeline).withTexture(...).createRenderSetup()
  RenderType.create(name, setup)
```

---

## 2. 成本模型

### 2.1 每帧渲染成本公式

```
每帧总成本 = E × [ 动画Tick成本 + Σ_{c∈Components} ( resolveOutput + 骨骼遍历 + 顶点变换 + 顶点写入 ) ]
```

其中：
- E = 可见实体数（基线 ~83）
- Components = 每个实体的模型组件数（通常 1-3）

### 2.2 动画 Tick 成本 (每实体)

```
BrAnimator.tickAnimation()
  animation/BrAnimator.java:28-56
  → per animation entry: animation.tickAnimation(data, animations, scope, ticks, ...)
    → Molang 求值逐通道 (位置/旋转/缩放 各 3 分量)
    残留热点: Molang$Expr$*.evaluate ~588ms self-time (Opt6 后缩至固有成本)
  ModelRuntimeData 每个骨头的 Entry 含 3x Vector3f (position, rotation, scale)
```

### 2.3 骨骼变换成本 (每骨骼×每顶点)

**transformPos** (BakedModel.java:90-102):
```
手动展开 Matrix4f × vec4:
  result[i*3]   = m00*x + m10*y + m20*z + m30    // 3 mul + 2 add
  result[i*3+1] = m01*x + m11*y + m21*z + m31    // 3 mul + 2 add
  result[i*3+2] = m02*x + m12*y + m22*z + m32    // 3 mul + 2 add
总计: 9 mul + 6 add + 数组读/写 = ~15 FMA 当量/顶点
```

**transformNormal** (BakedModel.java:104-122):
```
手动展开 Matrix3f × vec3:
  rx = m00*nx + m10*ny + m20*nz    // 3 mul + 2 add
  ry = m01*nx + m11*ny + m21*nz    // 3 mul + 2 add
  rz = m02*nx + m12*ny + m22*nz    // 3 mul + 2 add
  lenSq = rx² + ry² + rz²           // 3 mul + 2 add
  inv = 1/sqrt(lenSq)                // 1 sqrt
  normalResult[i*3] = rx * inv       // 1 mul
  等
总计: 9 mul + 6 add + 3 mul* + 1 sqrt + 3 mul = ~22 FMA 当量 + 1 sqrt/顶点
* lenSq 分支：如果 lenSq≤1.0E-8 跳过 normalize (但数学上不会)
```

### 2.4 顶点写入成本 (每顶点)

```
VertexConsumerPort.vertex():
  <1.20.6: consumer.vertex(x,y,z,r,g,b,a,u,v,overlay,light,nx,ny,nz)  // 14 参数
  >=1.20.6: addVertex().setColor().setUv().setOverlay().setLight().setNormal()  // 链式调用
  每调用: 写入 ~48 bytes (NEW_ENTITY format = 固定布局)
  附加: setColor(r*255) 整数乘法 × 4
```

### 2.5 Draw Call 估计

| 版本 | 每实体 Draw Call | 合计 |
|------|------------------|------|
| 1.20.1 / 1.21.1 | ~2 (1 solid + 1 translucent) | ~166 calls/frame (83 实体 × 2) |
| 26.1.2 | ~1-2 (vanilla 按 RenderType 合并) | ~5-20 calls/frame (不同 RenderType 数) |

说明：
- ImmediateRenderSink: **每个实体**的每个 ModelComponent 提交后调 `endBatch()` → 真正 OpenGL draw call
- DeferredRenderSink: `submitCustomGeometry` 注册到 vanilla SubmitNodeCollector → 由 LevelRenderer `renderAllFeatures` 阶段按 RenderType 分组后 endBatch 统一绘制 → 跨实体合并 draw call

### 2.6 材质切换频率

**按 passOrder 分组**: EntityRenderOrchestrator.java:265-269
- SOLID / ALPHA_TEST → passOrder 0
- TRANSLUCENT / TRANSLUCENT_EMISSIVE / ADDITIVE → passOrder 1

各 ModelComponent 内部按 passOrder 排序；同一实体同 order 的组件「相邻遍历」但**不合并为一个 RenderSink.submit**。

材质排序粒度是**组件级**而非骨骼级——同一组件内的多个骨骼共用同材质时已合并；不组件间即使同材质也走独立 submit。

### 2.7 DFSModel 骨骼变换层级问题

**发现**: DFSModel 线性化将骨骼树展平为 PreBoneFrame→PostBoneFrame 序列。播放时：
```
PreBoneFrame(parent)  → pushPose + applyBoneTranslate(parent) + render
PostBoneFrame(parent) → popPose
PreBoneFrame(child)   → pushPose + applyBoneTranslate(child)   ← parent 变换已弹出栈！
```

子骨骼渲染时的 `last.pose()` **缺少父骨骼变换**，子骨骼变换仅获得模型级 rotateY(180°) + 子骨骼自身动画偏移。若模型使用的是平铺骨骼结构（父骨骼作为逻辑分组且不含 cube），则此问题不显；**对有父子层级依赖的模型会造成骨骼变换缺失**。

成本影响：无（这是正确性问题，非性能问题）。

---

## 3. 分配热点清单 (file:line)

### 3.1 确认的每帧分配

| # | 分配位置 | 文件:行 | 对象 | 频率 | 大小 |
|---|----------|---------|------|------|------|
| A1 | `new ArrayList<>(components)` | EntityRenderOrchestrator.java:275 | ArrayList | 每实体 | O(1-3) |
| A2 | `.stream()` lambda | EntityRenderOrchestrator.java:277 | Stream + Spliterator + 多个 lambda | 每实体 | ~5-8 对象 |
| A3 | `new AnimationEffects()` | EntityRenderOrchestrator.java:86, 286 | AnimationEffects | 每实体×Tick | 空容器 |
| A4 | `new AnimationEffects()` | AttachableItemRenderSetup.java:167 | AnimationEffects | 每 attachable×Tick | 空容器 |
| A5 | `ModelRuntimeData.EMPTY` 回退 | EntityRenderOrchestrator.java:286 | 返回 EMPTY 常量(不分配) | 见上 | 0 |
| A6 | `new PoseStack()` + locators | EntityRenderOrchestrator.java:234 | PoseStack | 每实体×手持物渲染 | ~4 行 Deque |
| A7 | `new Int2ObjectOpenHashMap<>()` | EntityRenderOrchestrator.java:195 (collectBindBones) | Int2ObjectOpenHashMap | 每实体×Tick | ~16 entry 容量 |
| A8 | `ByteBufferBuilder(786432)` + `immediate()` | EyelibLivingEntityRenderer.java:120-121 | ByteBufferBuilder + BufferSource | **每实体 (26.1.2)** | 786KB 堆外 |
| A9 | `new FramePlan(...)` ×3 ArrayList | FramePlan.java:15-17 | ArrayList × 3 | 每帧 | 各 ~1-2 |
| A10 | `new HashMap<>()` (locators) | CollectLocatorModelVisitor.java:46 | HashMap | 首次收集 locator | 可变 |
| A11 | `new Int2ObjectOpenHashMap<>()` (bones cache) | ModelVisitor.java:130 | Int2ObjectOpenHashMap | 每 RenderHelper | ~16 entry 容量 |
| A12 | `PoseStackPort.copy(pose)` | ModelVisitor.java:131 | PoseStack.Pose (含 Matrix4f+Matrix3f) | 每骨骼首次访问 | ~96 bytes |
| A13 | `AnimationComponent.effects` / `tickedInfos` 赋值 | EntityRenderOrchestrator.java:97-98 | 引用赋值(不分配) | 每实体 | 0 |
| A14 | `stream()` per face | ModelVisitor.java:72-73 (visitCube) | 2x stream(.map) | 每 cube 每面 | ~4 对象 |
| A15 | `new Entry()` in ModelRuntimeData | animation/ModelRuntimeData.java:36 | Entry (含 3x Vector3f) | 每骨惰性创建 | ~96 bytes |

### 3.2 静态/不分配路径

| # | 位置 | 原因 |
|---|------|------|
| bakedBone.positionResult[] + normalResult[] | TwoSideModelBakeInfo.java:150-155 | 烘焙时一次性 new，每帧 transformPos/transformNormal 只 mutate 不 new |
| BrMaterialResolver.resolveCache | BrMaterialResolver.java:30-50 | volatile static IdentityHashMap，每帧命中缓存 |
| BrRenderTypeFactory.CACHE/ PIPELINE_CACHE | BrRenderTypeFactory.java:36-38 | ConcurrentHashMap，每帧 computeIfAbsent 命中 |
| BakedModel (TwoSideModelBakeInfo) | TwoSideModelBakeInfo.java:60-80 | HashMap 缓存每 (modelName, texture, meshTexture) 三元组 |
| RenderModelVisitor.tPosition / tNormal | visitor/RenderModelVisitor.java:14-15 | static Vector4f / Vector3f，跨顶点复用 |

---

## 4. 优化机会清单

### OPT-R1: DFSModel 骨骼层级继承中断

- **文件**: `client/model/DFSModel.java:31-34` + `ModelVisitor.java:125-133`
- **现状**: DFSModel 将递归骨骼遍历线性化为 PreBoneFrame→PostBoneFrame 序列。播放时 PostBoneFrame popPose 后，子骨骼 PreBoneFrame 获得的 `poseStack.last()` 缺失父骨骼变换。
  ```
  PreBoneFrame(parent)  → pushPose + transform(parent)  ✓
  PostBoneFrame(parent) → popPose                         ✗ 消去父变换
  PreBoneFrame(child)   → pushPose + transform(child)     ✗ 栈顶已不含父变换
  ```
- **成本**: 扁平骨骼结构(多数 A&S 模型)下无表现；父骨骼有 cube 且子骨骼继承父变换时，子骨骼位置/旋转错误。
- **思路 A**: 在 DFSModel 播放时维护显式的父骨骼变换栈——不依赖 PoseStack push/pop，而是由 Frame 实现直接管理骨骼累积矩阵。
- **思路 B**: 改回原始递归 `ModelVisitor.visitBone()` 遍历，恢复 PoseStack 的自然 push/pop 嵌套——但失去 DFSModel 的 visitCube 按帧提前判断是否 visible 的能力。
- **思路 C**: PreBoneFrame 传入父骨骼变换矩阵（从 "bones" 缓存读取），applyBoneTranslate 在父变换基础上应用当前骨骼变换。
- **风险**: 思路 A/B 需改动 DFSModel 核心逻辑，影响所有 visitor。思路 C 改动范围最小但需验证缓存中父骨骼 Entry 在播放时确实可访问。

### OPT-R2: renderComponents stream 替换为 for 循环

- **文件**: `EntityRenderOrchestrator.java:274-306`
- **现状**: `.stream().filter(...).mapToLong(...).sum()` 每实体分配 Spliterator + 多个 lambda 表达式 + 装箱。
- **思路**: 替换为 `for (ModelComponent mc : components) { ... if (filter) { ... count++ } }`
- **风险**: 低。纯流水线替换。
- **预期节省**: 每实体 5-8 个对象分配 + stream 虚函数开销。

### OPT-R3: AnimationEffects 延迟分配/复用

- **文件**: `EntityRenderOrchestrator.java:86, 286`、`AttachableItemRenderSetup.java:167`
- **现状**: `new AnimationEffects()` 每实体每 tick，即使组件无动画素材。大部分 empty。
- **思路 A**: `ModelRuntimeData.tickAnimation()` 返回时若无效果则不 set effects；consumer 检查 null。
- **思路 B**: 复用 AnimationEffects 实例——`clear()` 后重新填充。
- **风险**: 中等。AnimationEffects 被多处持有引用（effects 字段被 animation system 写入），复用需确保 clear() 后的语义等价于新实例。思路 A 更安全。

### OPT-R4: 26.1.2 每实体 ByteBufferBuilder 复用

- **文件**: `bridge/client/render/EyelibLivingEntityRenderer.java:120-121`
- **现状**: 每实体 submit 创建 `new ByteBufferBuilder(786432)` + `MultiBufferSource.immediate(byteBufferBuilder)`，后续 `endBatch()` + `close()`。~786KB 堆外每实体。
- **思路**: 线程本地 ByteBufferBuilder 池（如 ThreadLocal<ByteBufferBuilder>），用前 discard() 清空，用后 close() 归还。但 vanilla 26.1.2 实现可能要求每次 submit 用新 buffer——需验证 EntityRenderState 生命周期。
- **风险**: 高。ByteBufferBuilder 内部是 NIO ByteBuffer，pooling 可能因残留数据交叉影响。需严格 reset + keep capacity 策略。

### OPT-R5: 按材质排序的批处理提交

- **文件**: `EntityRenderOrchestrator.java:287-306` + `ImmediateRenderSink.java:30-42`
- **现状**: ImmediateRenderSink 每个 ModelComponent render 后 `sink.flush()` 立即 endBatch → 产生大量细粒度 draw calls。1.20.1 下 83 实体 → ~166 draw calls。
- **思路**: 同一实体内所有 ModelComponent 先收集、按 RenderType 分组、再逐组 endBatch。但不是批量几何——MultiBufferSource.getBuffer 已为同一 RenderType 返回同一 VertexConsumer，写在同一 buffer。
- **重点**: flush() 时机。当前每个实体 renderComponents 调用 `sink.flush()`。更激进：整帧所有实体完成后再 endBatch——但这已不是 eyelib 能控制的（返回给 MC 后才 flush）。
- **风险**: 低。改 flush 时机可能影响渲染顺序（透明排序）。
- **实际影响**: 1.20.1 下真正的问题是 per-entity endBatch 切断了跨实体 buffer 合并机会。在 26.1.2 DeferredRenderSink 下已通过 submitCustomGeometry 自动合并，无需此事。

### OPT-R6: BakedBone.transformPos/Normal 向量化

- **文件**: `bridge/client/render/bake/BakedModel.java:90-122`
- **现状**: 手动逐顶点展开的 Matrix4f/Matrix3f 变换。JVM 自动向量化（auto-vectorization）对此模式良好——连续 float[] 读写 + 循环计数可推断。
- **思路**: 确认 JIT 是否自动向量化；若不（无法确认），用 Panama Vector API (Java 17 incubator) 或循环分块。
- **风险**: 高。Java 17 Vector API 为 incubator；自动向量化受多种因素影响（数组长度、JVM 版本）。手动向量化导致代码不可维护。
- **建议**: **暂时搁置**。ROI 不明确。先确认 JIT 是否已生成 SIMD 指令（perf stat -e fp_arith_inst_retired.256b_packed_single）。

### OPT-R7: 预计算 bone uniform matrices 替代逐顶点变换

- **文件**: `bridge/client/render/bake/BakedModel.java:90-122`
- **现状**: BakedBone 持有原始弹位置 position[]，每帧由 transformPos 用当前骨骼矩阵进行逐顶点变换。
- **思路**: 如果骨骼是刚性的（没有 morph/deformation），可以每帧只传一个 uniform matrix 到 shader，让 GPU 做顶点变换。但 eyelib 用 immediate mode VertexConsumer，不走管线缓冲区——无法传递 uniform。
- **风险**: 架构变更。需要改为每骨骼上传预变换位置到预分配的 GPU buffer，shader 中一次性读取。相当于全部改写顶点提交路径。
- **建议**: **搁置**。只有使用 VBO/IBO/MeshData 路径时才可行。当前 eyelib 的 immediate mode 架构不适合。

### OPT-R8: 预计算部分 PartVisibility

- **文件**: `client/entity/RenderControllerRuntime.java:29-40`
- **现状**: `evalPartVisibility()` 每帧 evaluate 每骨骼的 part_visibility Molang 条件。条件引用 frame-based molang (如 `!q.is_alive`) 时每帧结果可变，必须逐帧 eval。
- **思路**: 缓存上次结果，仅当 MolangValue 引用的变量变化时重新 eval。但需要检测变量依赖——复杂。
- **风险**: 中等。附加的依赖追踪成本可能超过 eval 自身。BE 规范要求逐帧完整 eval，缓存可能改变语义。
- **建议**: **搁置**。Molang `Expr.evaluate` 残留 ~588ms self-time（Opt6 后），是 Molang 引擎固有成本的最后一位，ROI 有限。

### 排除项

| 项 | 原因 |
|----|------|
| BrMaterialResolver 更多缓存 | Opt5 已全局缓存 IdentityHashMap，resolve 0ms self-time ✓ |
| ModelComponent 材质解析 | Opt4 已缓存 ✓ |
| BakedModel 烘焙缓存 | TwoSideModelBakeInfo 已缓存 ✓，烘焙是一次性操作不在热路径 |
| RenderType 缓存 | BrRenderTypeFactory.CACHE 已缓存 ✓ |
| VertexConsumerPort 优化 | 穿不过（vanilla 标准接口） |
| Matrix4f/Matrix3f 逐顶点手动展开 | 已是最简 FMA 序列，~15 op/顶点已接近理论下限 |

---

## 5. Attachable 渲染路径 (ADR-0019)

### 路径

```
ItemInHandRendererMixin.onRenderItem()
  mixin/client/ItemInHandRendererMixin.java:28-87
  → AttachableResolver.resolve(entity, stack)
    client/entity/AttachableResolver.java:27-36
  → AttachableItemRenderSetup.getOrPrepare(entity, hand, isFirstPerson)
    client/render/AttachableItemRenderSetup.java:53-92
  → AttachableItemRenderSetup.renderAttachable(rd, poseStack, buffer, entity, light, overlay)
    client/render/AttachableItemRenderSetup.java:176-198

HumanoidArmorLayerMixin.eyelib$onRenderArmorPiece()
  mixin/client/HumanoidArmorLayerMixin.java:42-75
  → 同上
```

### 与主实体渲染的共享路径

- **渲染**: 两者都调 `RenderHelper.start().render(rp, model, tickedInfos)` → 完全相同的 DFSModel + HighSpeedRenderModelVisitor + BakedBone transform + VertexConsumerPort 路径。
- **动画**: `AttachableItemRenderSetup.tickForEntity()` 调 `BrAnimator.tickAnimation()` — 相同路径。
- **材质解析**: 相同的 `ModelComponent.getRenderType()` + `BrMaterialResolver` 缓存。
- **差异**:
  - Attachable 有独立 RenderData (per slot per entity)，持有独立 MolangScope + ModelRuntimeData。
  - 不含 passOrder 排序（`renderAttachable` 不 sort components —— 但实际多组件情况罕见）。
  - 暂未使用 `RenderSink.submit()` 路径——直接 `RenderHelper.render()` 写 `MultiBufferSource.getBuffer()`。
  - 共享 `WeakHashMap<LivingEntity, EnumMap<EquipmentSlot, RenderData<ItemStack>>>` 缓存。

### 优化注意事项

- 主渲染的 DFSModel 层级问题（OPT-R1）同样影响 attachable。但 attachable 多为单骨骼模型，无影响。
- Attachable 缺少 `sink.flush()` 调用——依赖 bufferSource 在外部统一 endBatch。
