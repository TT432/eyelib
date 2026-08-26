# 渲染系统 GPU 化调研报告

> 日期：2026-08-26。目标：在**完整保留所有功能**的前提下，评估 eyelib 渲染系统各项工作搬到 GPU 计算的可行边界与路线。
> 证据分级：①一手源码（文件:行号）②项目文档（docs/）③官方资料（Mojang/NeoForge/Fabric）④社区资料 ⑤推断（显式标注 [INFERENCE]）。
> 调研方法：5 路并行代码盘点（实体渲染链/动画 Molang/缓冲几何/材质 shader/文档体系）+ vanilla 源码一手验证 + 官方资料核实。

---

## 1. 结论摘要

1. **当前 eyelib 实体渲染是「CPU 软件蒙皮 + 每帧全量顶点重发」**：每帧对每实体逐骨骼合成矩阵、逐顶点做矩阵乘（`transformPos`/`transformNormal`）并逐顶点写入 vanilla `BufferBuilder`，GPU 端只有 vanilla entity shader 的 MVP/光照/雾化。**GPU 端零骨骼矩阵、零自有 shader、零自有 VBO**。
2. **最大可搬项是顶点蒙皮**（C1）：烘焙几何是刚体骨骼绑定（每顶点恰好属于一根骨骼、无权重混合），天然适合「静态 VBO + 骨骼矩阵调色板（TBO/UBO）+ 顶点着色器蒙皮」。这一改造同时消除每帧顶点 CPU 变换、逐顶点写入、以及 BufferBuilder 动态增长/上传三大成本。
3. **Molang/动画求值不可搬 GPU**（C3，主结论为否定）：200+ 个 `query.*` 函数直接读 Java 侧 MC 实体实时状态，`variable./temp./this` 有顺序累加语义，箭头访问运行时切换宿主。Bedrock 原版同为 CPU 逐帧采样架构（ADR-0013），parity 不要求实现方式一致。
4. **版本窗口是关键战略变量**：≤26.1 为 OpenGL 3.2 core（一手验证 vanilla `Window.java` GLFW hints），无 compute shader/SSBO；**26.2 起 vanilla 切 Vulkan**（官方公告），compute/SSBO 全面解锁。26.1 的 blaze3d 已提供自定义 RenderPipeline（自定义 shader + UBO uniform）这一官方扩展点，是 GPU 蒙皮的最佳先行版本。
5. **既有性能工作已把 CPU 侧榨到「接近固有成本」**（spark 实测 Opt1-9 合入后 eyelib self 36μs/实体/帧，Molang 求值链占渲染 7.7%）；进一步收益只能从「消除每帧顶点重建」获得，即 GPU 化。
6. **前置硬依赖**：DFSModel 线性化打断父子骨骼层级继承的缺陷（OPT-R1）必须先定论并修复——GPU 蒙皮要求每骨骼完整世界矩阵。【勘误 2026-08-27：OPT-R1 已于 2026-07-25 定论——缺陷不存在，帧序列由递归 visitBone 生成故 Pre/Post 严格嵌套、子骨骼正常继承父变换，可执行证据 DFSModelTest（da8634bd，1.20.1 复测绿）。此前置已解除。】

---

## 2. 现状盘点

### 2.1 实体渲染全链路数据流

```
每渲染帧（非每 tick）：
RenderLevelStageEvent(AFTER_SKY / AfterOpaqueBlocks)
  → RenderStageEventAdapter (bridge/.../RenderStageEventAdapter.java:43-56 / :58-71)
  → EntityRenderOrchestrator.onRenderStage (client/render/EntityRenderOrchestrator.java:133-137)
  → FramePipeline: SetupStage(:146-154) → EffectCommitStage → TickStage(:161-215)
       TickStage 逐实体: scope.set(partial_tick/attack_time) (:194-195)
         → BrAnimator.tickAnimation (animation/BrAnimator.java:28-60，时钟=(tick+partialTick)/20)
             → BrClipExecutor.tick (animation/bedrock/BrClipExecutor.java:26-109)
                 逐骨骼逐通道: 空通道短路(:53-56) → floorEntry/higherEntry 找关键帧
                 → linearLerp 每通道 6 次标量 eval / catmullromLerp 最多 12 次
                 → 结果累加进 ModelRuntimeData.Entry(position/rotation/scale)
         → tickedInfos 挂 AnimationComponent (:214)

实体级入口：
1.20.1/1.21.1: LivingEntityRenderer.render → RenderLivingEvent.Pre
  → RenderLivingEventAdapter.onEvent (bridge/.../RenderLivingEventAdapter.java:56-64)
26.1.2: 同事件，render-state 反查实体 findEntityByRenderState 线性扫描 (:90-111)
  → EntityRenderOrchestrator.renderEntityFromParams (:159-174) → renderComponents (:271-349)
       组件 passOrder 两档排序 (SOLID/ALPHA_TEST=0 先, 其余=1 后; :387-400)
       → resolveOutput (:662-690; usesColorMask 时生成颜色掩码派生纹理 :670-681)
       → RenderSink.submit(renderPass, texture, poseStack, GeometryWriter) (:438-445)
           <26.1 ImmediateRenderSink: getBuffer→同步写, flush=endBatch（每组件每实体一次）
           ≥26.1 DeferredRenderSink: collector.submitCustomGeometry 延迟到 renderAllFeatures，
                                    同 RenderType 跨实体合并 draw call
       → writer 内 RenderHelper.render (client/render/RenderHelper.java:71-93)
           → ModelBakePort.twoSideGetBakedModel（烘焙缓存命中）
           → DFSModel.visit(RENDER_WITH_LOCATOR) (client/model/DFSModel.java:31-34)
               → HighSpeedRenderModelVisitor.visitPreBone (visitor/HighSpeedRenderModelVisitor.java:22-41)
                   → applyBoneTranslate (visitor/ModelVisitor.java:133-153)
                       → ModelPoseTransforms.applyBone (animation/ModelPoseTransforms.java:24-42)
                           translate(bind+anim)→+pivot→rotateZYX→scale→−pivot，写入 PoseStack 栈顶
               → renderBakedBone (:43-52)
                   → BakedBone.transformPos(last.pose())  (bake/BakedModel.java:90-102，逐顶点 9mul+6add)
                   → BakedBone.transformNormal(last.normal()) (:104-122，逐顶点 9mul+6add+1sqrt 归一化)
               → visitVertex (:54-77) 逐顶点 VertexConsumerPort.vertex (VertexConsumerPort.java:11-30)
                   → vanilla VertexConsumer（<1.20.6 vertex(14参) / ≥1.20.6 addVertex() 链式）
```

旁路渲染路径（各自独立写顶点，未共享缓冲策略）：
- 粒子：`BedrockParticleRenderer.render` 每粒子每帧 CPU 算 billboard 四角 + 写 4 顶点（bridge/particle/adapter/BedrockParticleRenderer.java:135-171）
- attachable/盔甲/手持：`AttachableItemRenderSetup.renderAttachable` 经 `RenderParams.builder` 直接 `getBuffer`（RenderParams.java:57-65, 141-159；AttachableItemRenderSetup.java:224-246）
- GUI 预览：`TwoSideModelBakeInfo.drawGuiPreview*` Tesselator 即时 `BufferUploader.drawWithShader`（TwoSideModelBakeInfo.java:311-384）
- 方块/物品 bedrock 几何（**<26.1 only**）：`BrBlockGeometryLoader`(eyelib:bedrock) → `BrBlockUnbakedGeometry.addQuads` → `BakeVisitor` → BakedQuad → vanilla BakedModel → **vanilla 区块网格静态上传**（26.1 渲染管线重写后未适配，BrBlockGeometryLoaderHooks.java:38-47 注释明示不注册）

### 2.2 每帧 CPU 工作清单与成本模型

既有成本模型（work/perf-ms-frame-plan/PLAN.md §1.3，代码结构推得并已抽验）：

```
帧成本 = E × [ 动画tick + Σ_components ( resolveOutput + 骨骼遍历 + 顶点变换 + 顶点写入 ) ] + flush/draw
动画tick = A × ( 2×entry级eval + B × 3通道 × ( TreeMap查找×2 + 3轴 × (scope.this + Expr.evaluate) ) )
顶点变换 = B × V × ( ~15 FMA transformPos + ~22 FMA + 1 sqrt transformNormal )
```

实测基线：
| 指标 | 数值 | 来源 |
|---|---:|---|
| fbo-mixed-n96 render_work P50 | 8.57 ms | clientBenchmark run-20260724-231836 |
| world-mixed-n96 render_work P50 | 46.73 ms（含世界渲染+GL 等待） | 同上 |
| eyelib self（Opt6 后，83 实体稳态） | 2964ms/60s ≈ 36 μs/实体/帧，较基线 -71% | docs/perf/spark-baseline-and-optimizations.md |
| Molang 求值链稳态 | ~4600ms/60s = 渲染时间 7.7% | 同上 :55 |
| 96 实体每帧 `this` 写入 | ~21,600 次（Opt7 前；已优化为专用字段） | 同上 :66 |

顶点规模：每 cube = 6 面 × 4 顶点 = 24（无索引，QUADS）；双面材质翻倍；texture_mesh 按不透明 texel 体素化为 1×1×1 迷你立方体（TwoSideModelBakeInfo.java:217-301），顶点数可远超 cube 数。

### 2.3 几何烘焙与缓冲生命周期

| 路径 | 上传频率 | 缓冲来源 | 重建触发 |
|---|---|---|---|
| 实体几何（<26.1 立即 / ≥26.1 延迟提交） | **每帧** | vanilla BufferSource/共享 BufferBuilder；26.1 fallback 路径每实体 `new ByteBufferBuilder(786432)`（EyelibLivingEntityRenderer.java:120-121） | 每帧（动画） |
| BakedModel/BakedBone 烘焙数据 | 静态 CPU 缓存（float[]），**不进 GPU** | 懒烘焙 `computeIfAbsent`，bakedCache key=(model, texture, meshTexture)（TwoSideModelBakeInfo.java:102-105） | ModelManager 条目变更/批量替换（ModelBakeInvalidationHooks.java:16-41） |
| **BakedBone 构造时预建的 `BufferBuilder vertices`**（BakedModel.java:30-96） | **从未上传、从未被读取——死负载** | — | — |
| 方块几何（<26.1） | 静态（进区块 VertexBuffer） | vanilla BakedModel quads | chunk 重建/模型重载 |
| 粒子 billboard | 每帧 | 全局 bufferSource | 每帧 |
| AR 加速 mesh（可选，<26.1） | 一次 build + 每帧 write | AR 静态 IMesh 缓存（AcceleratedBakedBoneRenderer.java:29-66） | 骨骼/缓冲图变化 |

关键事实：**全库 0 处直接使用 `VertexBuffer`/VBO**；GPU 上传完全委托 vanilla endBatch/RenderPass（「薄映射层」原则，docs/concepts/cross-version-render-architecture.md:394-398）。

### 2.4 GPU 端现状

- **实际渲染全部走 vanilla entity shader/PSO**：<26.1 经 `GameRenderer.getRendertypeEntity*Shader()`（BrRenderTypeFactory.java:136-146）；≥26.1 经 `RenderPipelines.ENTITY_SNIPPET`/`ENTITY_EMISSIVE_SNIPPET`（BrRenderTypeFactory.java:243-246）。
- GPU 承担：MVP 变换、漫反射光照（Normal 由 CPU 每帧变换+归一化后传入）、fog、overlay/lightmap texelFetch、贴图采样、cutout discard、混合/模板/深度静态状态。
- **无**：UBO/SSBO/instancing/TBO/compute shader/自有 VBO（全库 grep 零命中）。Java 侧无任何 glUniform/ShaderInstance 调用，uniform 全部由 vanilla 绘制时上传。
- **死资产**（可复用接缝）：3 组 `assets/eyelib/shaders/core/*_chunk` shader（passthrough + ChunkOffset，Java 零引用；注意 1.20.1 中 mod 命名空间 core shader 不会被 vanilla 自动加载，需 RegisterShadersEvent 显式注册）；`ShaderManager`（ARB GLSL 编译缓存，生产零调用者）；`shader_mapping.json`（零引用）。
- 26.1.2 已具备的 GPU 侧进步：PSO 预编译缓存（PIPELINE_CACHE）、`sortOnUpload` 排序标记、DeferredRenderSink 跨实体合批。

### 2.5 直接 GL 调用清单（GPU 化兼容性约束）

| 位置 | 调用 | 版本 |
|---|---|---|
| ShaderManager.java:40-97,149-162 | ARB 着色器对象全套（死代码） | 全部 |
| NativeImageIO.java:128-131 | glBindTexture + glGetTexImage 纹理回读 | 全部 |
| GuiRenderBridge.java / MCGraphics.java:177-199 | RenderSystem.enableBlend/setShaderColor（26.1 抛 UnsupportedOperationException） | <26.1 |
| BrRenderTypeFactory.java:164-172 | RenderSystem.enableBlend + GL14.glBlendFuncSeparate（state shard lambda） | <26.1 |
| material/gl/* 枚举 | GL11/GL14/GL46 常量（状态描述） | 全部 |
| 26.1 材质路径 | 无直接 GL，全部声明式 PSO | ≥26.1 |

### 2.6 版本差异矩阵（渲染相关）

| 层 | 1.20.1 / 1.21.1 | 26.1.2 |
|---|---|---|
| GL/Vulkan | OpenGL 3.2 core（一手验证，见 §3.1） | 同左（26.1 是最后纯 OpenGL 版本；26.2 转 Vulkan，见 §3.3） |
| 提交模型 | ImmediateRenderSink：getBuffer + 每组件 endBatch（每实体 ~2 draw call） | DeferredRenderSink：submitCustomGeometry，vanilla renderAllFeatures 按 RenderType 合批 |
| RenderType | RenderType.create + CompositeState 状态机 | RenderPipeline PSO + RenderSetup + PIPELINE_CACHE |
| 实体入口 | RenderLivingEvent.Pre 直接携带实体 | render-state 反查实体（线性扫描） |
| 自定义着色扩展点 | Forge RegisterShadersEvent → ShaderInstance → CompositeState.shaderStateShard | RenderPipeline.builder().withVertexShader/withFragmentShader/withUniform(UNIFORM_BUFFER)（官方 primer 示例） |
| 顶点几何计算路径 | **完全相同**（DFSModel + HighSpeedRenderModelVisitor + BakedModel 变换） | 同左 |

---

## 3. 外部约束调研

### 3.1 OpenGL 版本：3.2 core（一手验证）

vanilla 1.20.1 `com/mojang/blaze3d/platform/Window.java:80-85`（forge-1.20.1-47.4.16_mapped_parchment sources jar）：

```java
GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
GLFW.glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
GLFW.glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
GLFW.glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
GLFW.glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
```

③ Mojang 系统要求自 1.18 起为 OpenGL 3.2 core；mods.toml 模板注释亦声明 "GL version 3.2 or higher"（src/main/templates/META-INF/mods.toml:94-98）。1.21.1 同（blaze3d 此期间未变）。

**GL 3.2 core 能力边界（对 GPU 化的含义）**：

| 特性 | 版本要求 | ≤26.1 可用性 | 对 eyelib 的用途 |
|---|---|---|---|
| UBO（uniform buffer object） | GL 3.1 core | ✅ | 骨骼矩阵调色板（GL 保证 ≥16KB/块 = 256 mat4，实际 64KB） |
| TBO（buffer texture，texelFetch） | GL 3.1 core | ✅ | 骨骼矩阵调色板（无尺寸焦虑，`samplerBuffer`+`texelFetch`） |
| 实例化绘制 glDraw*Instanced | GL 3.1 core | ✅ | 粒子/重复几何 |
| 顶点属性除数 glVertexAttribDivisor | GL 3.3 core / ARB_instanced_arrays | ⚠️ 扩展依赖（桌面普遍有；macOS GL 4.1 core 有） | 每实例数据（实体 id→调色板偏移） |
| Transform feedback | GL 3.0 core | ✅ | GPU 端矩阵合成回写（收益低，见 C5） |
| Geometry shader | GL 3.2 core | ✅ | billboard 展开（备选；性能口碑差） |
| **Compute shader** | GL 4.3 core | ❌ | 不可用 |
| **SSBO** | GL 4.3 core | ❌ | 不可用 |
| 多纹理数组 texture2DArray | GL 3.0 core | ✅ | 材质图集化 |

注：文档 `docs/concepts/alpha-clamp-two-path.md:38` 提及「TextureLayerMerger.merge() 的 compute shader」，但**全库代码 grep 无 TextureLayerMerger/GL_COMPUTE_SHADER 任何实现**（仅 LDLib2 的 Shader.Type 枚举支持 COMPUTE 类型）——文档与代码不一致，历史上似乎考虑过 compute 但未落地或已移除。这佐证 ≤26.1 不应依赖 compute。

### 3.2 26.1.2 blaze3d GPU API 能力（一手：docs/vanilla_research/geo_render/26.1.2.md + ③ NeoForge 26.1 primer）

- `RenderPipeline`（PSO）：`withVertexShader/withFragmentShader/withSampler/withUniform(name, UniformType.VEC3|INT|UNIFORM_BUFFER)/withShaderDefine/withColorTargetState/withDepthStencilState/withCull`——**自定义 shader + UBO 是官方扩展点**（NeoForge primer 给出 examplemod 自定义管线完整示例）。
- `DynamicUniforms`/`DynamicUniformStorage`：std140 UBO 环形缓冲，per-draw transform 以 `GpuBufferSlice` 引用（26.1.2.md:1049-1082）——vanilla 自带的「每绘制数据打包上传」范式，骨骼调色板可同构实现。
- `GpuDevice`/`GpuBuffer`/`GpuBufferSlice`/`UberGpuBuffer`（TLSF 大缓冲 + 批量上传）。
- 实体两阶段：extractRenderState → submit → SubmitNodeCollector → FeatureRenderDispatcher 按 RenderType 合批绘制（renderSolid/renderTranslucent）。
- **无 compute pipeline**：primer 与 vanilla 研究文档均无 compute 类；③ Mojang Vulkan 公告把 compute shader 列为 Vulkan 迁移后才解锁的能力，反向印证 GL 管线没有。

### 3.3 26.2 起转 Vulkan（③官方）

- Mojang 官方文章《Minecraft Java Edition 26.2》：当前要求 **Vulkan 1.2 + dynamic rendering + push descriptors**。
- 《Another step towards Vibrant Visuals for Java Edition》：OpenGL→Vulkan 切换，明确提及 compute shaders 等能力。
- Fabric 官方博客（2026-03-14）：**26.1 是最后一个仅支持 OpenGL 的版本**，26.2 snapshot 起允许后端切换。

含义：任何「必须 compute/SSBO」的设计都应排期到 26.2+ 节点；26.1.2 及以前的设计必须限于 GL 3.2 能力集（§3.1）。

### 3.4 Bedrock 端对照

- ADR-0013（docs/decisions/0013-bedrock-animation-controller-and-calculation.md）：Bedrock `ActorSkeletalAnimationPlayer`/`BoneAnimationChannelPlayer` **每帧 CPU 逐帧采样**，eyelib 与 BE 同为 CPU 架构（无 GPU 皮肤）；已确认硬语义：每帧 bind pose 重置 + 动画逐通道累加、关键帧 linear/catmullrom 插值、`this` 逐轴绑定。
- RenderDragon（BE 渲染引擎，bgfx 系，DX/GLES/Metal/Vulkan 后端）引擎侧是否 GPU 蒙皮：社区反编译资料未给出一手证据 [INFERENCE: bgfx 惯用法是 CPU 算骨骼矩阵→uniform 上传→VS 蒙皮；BE 顶点计算在哪一端不影响本调研结论]。
- **结论**：Bedrock parity 约束的是**结果**（顶点位置、材质行为、绘制顺序），不是实现位置。GPU 蒙皮不违反 parity，前提是顶点数学逐位一致（rotateZYX 顺序、pivot 处理、bind+offset 语义）。

### 3.5 同类实践

- Sodium/Iris：实体渲染保持 CPU 变换（vanilla 兼容优先）；Iris 管线把实体交给 shaderpack 的 VS，但不替实体做骨骼调色板。
- 项目已集成的 AR（acceleratedrendering，<26.1 only）：唯一落地的「静态 mesh 上传一次 + 每帧 write」范式（AcceleratedBakedBoneRenderer.java:29-66），但其变换在 AR 管线内，与自有 shader 蒙皮路径互斥（ARBakedVisitor.java:25-33）。
- vanilla 26.1.2 自身的 `drawMultipleIndexed + UberGpuBuffer + DynamicUniforms UBO` 是「几何静态驻留 GPU + per-draw 数据动态上传」的官方范式——C1 设计与之同构。

---

## 4. 功能保留清单（硬约束，GPU 化不得破坏）

来自 DocsScout 对 specs/gap-analysis/concepts 的盘点（证据：docs/concepts/bedrock-parity-investigation.md:42-61、cross-version-render-architecture.md 不变量 I1-I4）：

1. 材质继承归并 + defines/states 推导 + Transparency 分类（SOLID/ALPHA_TEST/TRANSLENT/…）。
2. `materials` 分区覆盖语义（后覆盖前，GPU pass 数 = 唯一材质数；bone-level-material-rendering.md）。
3. passOrder 两档排序（不透明/cutout 先于半透明/加法/emissive）+ cutout 先写的深度写入顺序。
4. emissive alpha 是发光掩码；低 alpha 需 clamp 且 **clamp 必须在 merge 前**（alpha-clamp-two-path.md:37-38）。
5. additive 混合 (SourceAlpha, One)；alphatest 双路径纹理（complex:clamped/）。
6. overlay_color / tint(rcColor) 顶点着色；partVisibility 逐骨骼可见性（BE 规范逐帧求值）。
7. locator/attachable 锚点：`resolveLocatorPose` 是唯一权威变换链（bedrock-parity-investigation.md 禁止第二套骨骼变换实现）——**CPU 侧必须始终能拿到每骨骼世界矩阵**。
8. texture_meshes 体素化、texture.material 逐 pass 求值、RC 条件每帧重估。
9. 动画语义：每帧 bind pose 重置 + 逐通道累加、`this` 逐轴、anim_time_update/blendWeight/blend_transition 等。
10. attachable/盔甲/手持渲染路径、GUI 预览路径、粒子（含 bind_to_actor、动画帧序）。
11. 跨版本行为对齐（三版本 RenderSink 兼容层）；ArchUnit 分层（bridge/domain 隔离，ADR-0010/0016）；不引入跨线程同步（ADR-0019）。

---

## 5. GPU 化候选逐项分析

### C1 · 实体顶点蒙皮 → 顶点着色器（核心项，收益最大）

**搬什么**：`BakedBone.transformPos/transformNormal`（每帧每顶点 ~37 FMA + 1 sqrt + 48B 写入）+ 每帧全量顶点重发 + BufferBuilder 动态增长/上传。

**目标数据流**：

```
加载期（一次）：BakedBone 烘焙 → 静态 indexed 几何（每顶点带 boneIndex 属性）
              → 上传 VBO/IBO（≤26.1 用 vanilla VertexBuffer；26.1.2 用 GpuBuffer）
每帧：TickStage 动画求值（不变，CPU）
  → 渲染期：每骨骼 applyBone 合成世界矩阵（不变，CPU，量小）
  → 收集矩阵数组 → 上传骨骼调色板（TBO RGBA32F 或 DynamicUniforms 风格 UBO）
  → 每实体一次 draw：VS 按 boneIndex texelFetch 调色板做蒙皮 + normal 变换归一化
```

**为什么可行**：
- Bedrock 模型是**刚体骨骼绑定**（cube 整体属于一根骨骼，无权重混合、无 morph）——蒙皮退化为每顶点一次矩阵乘，VS 实现简单且与 CPU 数学逐位同构（同一 rotateZYX 公式）。
- 骨骼矩阵合成本来就必须留在 CPU（locator/attachable 需要世界矩阵读回，§4.7），调色板只是其副产物，零额外 CPU 成本。
- 26.1.2：自定义 RenderPipeline + `withUniform(UNIFORM_BUFFER)` 是官方路径；per-entity 数据走 DynamicUniforms 同构的 ring buffer。
- ≤26.1：Forge `RegisterShadersEvent` 注册自定义 ShaderInstance + `RenderType.create` CompositeState shaderStateShard；vanilla `VertexBuffer` 类可直接用（保持「薄映射层」精神：不自建裸 GL，用 vanilla 缓冲抽象）；骨骼调色板用 TBO（GL 3.1，无 UBO 尺寸焦虑，VS texelFetch）。
- 3 组孤儿 `*_chunk` shader 与 `ShaderManager`/shader_mapping.json 是现成接缝（需重命名并扩展骨骼属性）。

**功能保留要点**：
- partVisibility：骨骼级可见性 → 几何按骨骼分段（draw range 按骨骼组切），可见性变化只改 draw 段集合，不改几何；或调色板中存 visibility flag 由 VS 把顶点坍缩到裁剪空间外（退化三角形，驱动剔除）。前者更干净。
- 材质分区/passOrder/透明排序：组件→RenderType 映射不变，提交仍走 RenderSink；排序仍委托 vanilla（26.1.2 sortOnUpload）。
- 法线：VS 内 mat3 变换 + normalize（sqrt 搬 GPU）；光照方向 uniform 不变。
- overlay/lightmap：vanilla shader 语义需在自定义 shader 中复刻（现有孤儿 shader 已含 texelFetch overlay/lightmap 参考实现）。
- 粒子 locator、attachable、AR 兼容：CPU 矩阵数组是唯一权威，三者的消费方式不变。

**前置条件**：
1. **定论并修复 OPT-R1**（DFSModel 线性化 popPose 打断父子骨骼层级继承，work/perf-ms-frame-plan/render-scout-report.md:276-279）——GPU 蒙皮要求每骨骼完整世界矩阵，该缺陷不修复则 GPU 化无从谈起。【勘误 2026-08-27：已定论为侦察报告误判，无需修复，见 DFSModelTest（da8634bd）。】
2. 顶点格式扩展：NEW_ENTITY 无 boneIndex 属性，需自定义 VertexFormat。
3. 像素级回归基建：clientsmoke FBO 像素对比 + RenderDoc GetPostVSData 顶点数/坐标验证（docs/concepts/entity-verification-workflow.md 已有方法）。

**风险**：浮点一致性（CPU double 中间值 vs GPU float——需 RenderDoc GetPostVSData 数值对比定容差）；AR 兼容分支互斥（AR 装时回退现有路径）；1.20.1 合批（R1 子任务）与 GPU 蒙皮的顺序——先做 R1 合批收益即时可见，C1 是更大重构。

**预期收益**：消除每帧 `E × B × V` 的顶点 CPU 变换与写入；draw 数据量从「每帧全量顶点上传」降为「每帧骨骼矩阵上传」（典型实体骨骼数 << 顶点数，上传量降 1-2 个数量级）[INFERENCE: 具体倍数取决于模型顶点/骨骼比，需 benchmark 验证]。

### C2 · 提交路径直写裸缓冲（CPU 优化，C1 的前置/平行项）

逐顶点 `VertexConsumerPort.vertex`（14 参 / addVertex 链式 + setColor ×255 转换）→ 改为变换结果直接写裸 off-heap ByteBuffer，整块 `putBulkData`/memcpy 进 BufferBuilder。26.1.2 可用 `UberGpuBuffer`/ring buffer 同构。即使 C1 落地前，此项也独立减少每帧 CPU 与分配成本（PLAN.md R1/R3 相关）。风险低。

### C3 · Molang/动画求值 → GPU（**结论：不可行，不搬**）

证据（AnimScout，行号级）：
- ~200+ 个 `@MolangFunction` query 直接读 Java 侧 MC 实体实时状态（bridge/molang/MolangBuiltInQuery.java:115-1484；HostContext 塞 Entity/LivingEntity，EntityPortAdapter.java:64-67）；`MolangQuery` 读 DataAttachment/Minecraft.getInstance()（camera/biome/level）。
- `variable./temp.` 存 MolangScope 的 Map 且跨表达式顺序累加有语义；`this` 逐轴绑定（bind+已累积值换算，BrClipExecutor.java:64-101）；箭头访问（`->`）运行时切换宿主上下文（MolangRuntimeSupport.java:97-105）。
- 控制流（loop/for_each/break/continue）已编译为 JVM 字节码；搬 GPU 需在 shader 里重写解释器+回调桥，工程成本巨大且每帧还要把 200+ 种实体状态打包上传。
- 收益侧也不支持：Molang 求值链优化后仅占渲染时间 7.7%，且「Expr.evaluate 接近 Molang 固有成本」（spark 文档 :132）。
- BE 原版同为 CPU 逐帧采样（ADR-0013）。

**部分可行子集**（仅记录，不建议排期）：无 query 依赖的纯关键帧插值（life_time 驱动）理论上可 GPU 化，但 `query.life_time` 本身就是 query——剔除 query 后所剩无几，语义风险大于收益。

### C4 · 粒子 billboard → 实例化/VS 展开

现状：每粒子每帧 CPU 算四角 + 写 4 顶点（BedrockParticleRenderer.java:135-171）。
方案：静态单位 quad VBO + 每粒子实例数据（位置/尺寸/旋转/UV/颜色）经实例化绘制（GL 3.1 glDraw*Instanced；divisor 依赖 ARB_instanced_arrays，桌面普遍可用）或 26.1.2 自定义管线。VS 内做 camera-facing 展开。
功能保留：粒子材质 RenderType 缓存（WeakHashMap）、bind_to_actor（位置输入仍来自 CPU 实体状态——每帧上传实例数据不可避免，但上传量 = 粒子数 × 小结构，远小于 4 顶点展开）。
收益：粒子密集场景；优先级次于 C1。

### C5 · 骨骼矩阵合成 → GPU（**结论：不搬**）

- 量级：每实体每帧骨骼数×1 次矩阵合成（~15-60 个 4×4），CPU 成本远小于顶点变换。
- locator/attachable/AR 必须 CPU 读回世界矩阵（§4.7），GPU 化还需回读，得不偿失。
- ≤26.1 无 compute；transform feedback 方案复杂度高于收益。
- 26.2+ Vulkan 后可重估（与 C1 的调色板上传合并为 compute 预合成），但预期收益仍低。

### C6 · 派生纹理（clamped/colorMask）→ GPU 离屏渲染

现状：clamped 副本 = NativeImage download → CPU clampAlphaToBinary → upload（RenderControllerEntry.java:430-441）；colorMask 副本同理（NativeImageIO.java:227-233）。CPU 像素操作且纹理身份进渲染状态 key，破坏批处理/PSO 复用。
方案：离屏 fragment shader 渲染到纹理（GL 3.2 FBO 足够，无需 compute）；colorMask 更优解是改成自定义 shader 的 tint uniform（消除派生纹理本身，需与 C1 的自定义 shader 协同）。
注意：TextureLayerMerger 的 compute shader 只存在于文档，代码无实现（§3.1 注）——若复活纹理合并需求，≤26.1 必须用 FBO 离屏而非 compute。

### C7 · 方块/物品 chunk 路径（**无需 GPU 化**）

已是静态烘焙进 vanilla 区块网格（<26.1），无每帧成本。真实缺口是 26.1 未适配（BrBlockGeometryLoaderHooks 不注册），属于功能对齐问题而非 GPU 化问题。

### C8 · 透明排序/视锥剔除（**不动**）

实体级剔除与同 RenderType 内距离排序完全委托 vanilla；自建 GPU 排序/剔除会破坏与 vanilla 世界的深度/混合交错，风险远超收益。

---

## 6. 分阶段路线图

| 阶段 | 内容 | 版本 | 验证 |
|---|---|---|---|
| **P0 前置** | ①~~定论+修复 OPT-R1~~（已完成：定论无缺陷，DFSModelTest da8634bd）②~~决策死资产~~（已完成 70bea805：3 组 *_chunk shader/ShaderManager/shader_mapping.json 共 19 个零引用死文件全部删除，alpha-clamp-two-path.md 死引用已勘误）③C2 直写缓冲 | 全版本 | clientsmoke FBO 像素回归 + benchmark |
| **P1 先行** | C1 GPU 蒙皮：自定义 RenderPipeline + UBO/TBO 调色板 + 静态 GpuBuffer 几何 | **26.1.2**（API 最友好：官方自定义管线 + DynamicUniforms 范式 + DeferredRenderSink 合批） | **已完成 2026-08-27（1d47d353/88da7479/c957789a）**：UBO 调色板（TBO 无 float 格式，弃用）+ drawMultipleIndexed 阶段批量 flush。正确性验证通过；benchmark 结论**性能中性**（26.1.2 瓶颈在 vanilla submit 机制）。详见 docs/perf/c1-gpu-skinning-26.1.2.md |
| **P2 跟进** | C1 移植 ≤26.1：RegisterShadersEvent + 自定义 VertexFormat(boneIndex) + vanilla VertexBuffer + TBO 调色板；与 R1 合批协同 | 1.20.1 / 1.21.1 | 同 P1，三版本行为对齐。**待用户决策是否启动**（P1 中性，但 ≤26.1 顶点 CPU 占比高，预期收益更实） |
| **P3 扩展** | C4 粒子实例化；C6 派生纹理 GPU 化/colorMask uniform 化 | 全版本 | FBO + benchmark |
| **P4 平台跃迁** | 26.2 Vulkan 节点落地后重估：compute 调色板预合成、SSBO、GPU 蒙皮 compute 化 | 26.2+ | 同左 |

每个阶段独立可验证、独立提交（沿用 PLAN.md 的测量纪律：benchmark 期间禁 spark/RenderDoc；fresh JVM；baseline/candidate 交错）。

## 7. 关键风险登记

1. **OPT-R1 未定论**：若当前模型实际依赖层级继承（A&S 多为扁平骨骼可能掩盖），C1 之前必须修复并回归。【已解除 2026-08-27：定论无缺陷，DFSModelTest（da8634bd）1.20.1 绿。】
2. ~~**薄映射层原则冲突**~~【已解除 2026-08-27：ADR-0032 显式修订——允许经 vanilla 抽象（GpuBuffer/RenderPipeline）持自有 shader/几何，禁裸 GL 直调】。
3. **浮点一致性**：CPU 路径 PoseStack（JOML float）与 GPU fp32 蒙皮的舍入差异；验证方法已具备（RenderDoc GetPostVSData）。
4. **26.1.2 共享 buffer 中途 endBatch 的已知降级**（DeferredRenderSink.java:42-52）：自定义提交路径需避开或继承该守卫。【P1 实证：26.1.2 FBO 场景 writer 从不执行（clientsmoke 跳 renderAllFeatures）；瓶颈在 vanilla submit 机制本身，详见 docs/perf/c1-gpu-skinning-26.1.2.md】
5. **AR 兼容分支**互斥：ARBakedVisitor 需原始 bind-pose 数组，GPU 蒙皮启用时 AR 路径回退。【P1 已按此实现：AR 宿主自动回退 CPU 路径。】
6. **版本碎片化**：1.20.1 状态机 RenderType、1.21.1 过渡、26.1.2 PSO、26.2 Vulkan——四条轨道的适配成本随时间递增；建议 P1 只做 26.1.2 以最小化并行维护面。

## 8. 附录：证据索引

- 代码盘点：5 路 scout 报告（实体链/动画/缓冲/材质/文档），全部行号级，见本报告 §2/§5 内联引用。
- 既有侦察：work/perf-ms-frame-plan/PLAN.md、render-scout-report.md、anim-scout-report.md。
- 基线数据：docs/perf/spark-baseline-and-optimizations.md、docs/perf/render-fps-benchmark.md。
- vanilla 研究：docs/vanilla_research/geo_render/{1.20.1,1.21.1,26.1.2}.md。
- 一手验证：forge-1.20.1 sources jar `Window.java:80-85`（GL 3.2 core）。
- 官方资料：Mojang 26.2 发布公告（Vulkan 1.2）、Vibrant Visuals 公告、NeoForge 26.1 primer（自定义 RenderPipeline 示例）、Fabric 26.1 博客（最后纯 OpenGL 版本）。
- 架构约束：docs/concepts/cross-version-render-architecture.md（I1-I4、§9 薄映射层）、ADR-0010/0013/0016/0019。
