# Wintersky 融入 eyelib 设计

> **工作单元类型**：计划（设计规格文档）
> **前置**：wintersky 1.3.3 as-is 移植已完成（ADR-0034，`io.github.tt432.eyelib.wintersky` 模块，零 MC 依赖，Node golden oracle 逐 tick 对齐）
> **架构契约**：ADR-0002（模块边界）、ADR-0014（flat-merge）、ADR-0015/0016/0017（Stonecutter 多版本 + 四层模型 + `//?` 唯一栖息地）
> **范围**：设计如何将移植版 wintersky 运行时接入 eyelib 的客户端渲染与资源管线。本文档只做设计，不含实现。

---

## 1. 现状基线

### 1.1 移植产物（输入）

| 层 | 包 | 职责 | MC 依赖 |
|----|----|------|---------|
| wintersky 根包 | `eyelib.wintersky` | Scene/Config/Emitter/Particle/EventClass/内置纹理 | 零 |
| three r134 子集 | `eyelib.wintersky.three` | 向量/四元数/欧拉角/Matrix4/Object3D 场景图/曲线/BufferAttribute/Mesh/ShaderMaterial/Texture | 零 |
| molangjs 移植 | `eyelib.wintersky.molang` | Molang 表达式（曲线 variableHandler 桥接） | 零 |
| tinycolor2 子集 | `eyelib.wintersky.tinycolor` | 颜色解析/混合 | 零 |
| rng | `eyelib.wintersky.rng` | 可注入随机源 | 零 |

**渲染输出面**（wintersky 本身不做 GL 绘制，绘制原是 three.js WebGLRenderer 的职责）：

- 每个 `Particle` 持有一个 `three.Mesh`（PlaneGeometry 2×2，4 顶点），挂在其 `Emitter` 的场景图节点下；顶点色/UV 存于独立 `clr`/`uv` `Float32BufferAttribute`（每顶点 float32 r,g,b,a / u,v，逐 tick 重写）。
- 每个 `Emitter` 共享一个 `ShaderMaterial`：uniform `materialType`（0 alpha 裁剪 / 1 opaque / 2 blend / 3 additive）+ 纹理 + flipbook atlas 参数；`side=DoubleSide`；blend 模式与 `depthWrite` 由 materialType 推导。
- 片元语义：`tColor.rgb * vColor.rgb`；alpha 按 materialType 取 `discard<0.5` / `1` / `*vColor.a`。

### 1.2 既有 eyelib 集成面（复用对象）

| 既有件 | 位置 | 可复用性 |
|--------|------|----------|
| 事件挂钩模式 | `bridge/particle/ParticleRenderHooks`（RenderTick START / ClientTick START / AFTER_ENTITIES `<26.1` / LoggingOut clear） | 模式照搬，实例独立 |
| 每粒子渲染器 | `bridge/particle/adapter/BedrockParticleRenderer`（PoseStack push→translate→rotate→4 顶点；`vertex()` 静态 `//?` 屏蔽 14 参/链式） | `vertex()` 写法与 `//?` 边界照搬 |
| RenderType 解析 | `RenderTypeResolver.resolveParticle` + `RenderPassAdapter` | **不复用**（见 D2） |
| 管理器 | `particle/ParticleRenderManager`（emitters/particles 双表、判活、清理） | 模式参考，不共用实例 |

---

## 2. 定位与设计决策

### D1 双运行时共存，不合并

`eyelib.particle`（规范驱动组件化运行时，JOML + eyelib.molang 编译引擎）与 `eyelib.wintersky`（参考实现逐字移植）**长期共存**：

- **eyelib.particle** = 生产路径。组件化、可裁剪、走 material 管线、支持实体上下文 query。
- **eyelib.wintersky** = 参考精确路径。用途有二：
  1. **预览/编辑器级保真**：与 Blockbench/Snowstorm 视觉一致的效果预览（同一引擎同一语义）。
  2. **交叉校验 oracle**：同一 Bedrock 粒子 JSON 喂两个运行时，对比粒子计数/寿命/位置分布，作为 `eyelib.particle` 组件实现的规格强化测试（§6.3）。

**理由**：wintersky 的价值恰恰在于"未经重新设计的原样语义"（含 JS 怪癖）。把它重构进组件管线会丢失 oracle 属性；让生产管线依赖它则引入 molangjs 语义（角度制、无 `%` 运算符等）与 eyelib.molang 的冲突。

### D2 wintersky 渲染绕过 material 管线，专用 RenderType 映射

wintersky 的材质模型只有 `materialType × 纹理`，不含 Bedrock 材质继承/defines/states 语义。走 `BrRenderTypeFactory` 会引入无关的材质推导，破坏 as-is 保真。新增 `bridge/particle/adapter/WinterskyRenderTypes`，直接映射：

| materialType | wintersky GL 语义 | 1.20.1 RenderType 基座 | 备注 |
|---|---|---|---|
| 0 alpha | NormalBlending，`discard(a<0.5)` | `entityCutoutNoCull(tex)` 变体 | depthWrite on |
| 1 opaque | a=1 | `entitySolid(tex)` | |
| 2 blend | NormalBlending，depthWrite=**off** | `entityTranslucent(tex)` | |
| 3 additive | AdditiveBlending，depthWrite=**off** | 自定义 CompositeState（SrcAlpha/One） | vanilla 无现成粒子 additive 基座 [实现期核对选型] |

统一约束：`side=DoubleSide` → NoCull；光照恒 `FULL_BRIGHT`（wintersky 无光照概念，片元不吃 lightmap）；颜色 = `clr` attribute 原值（0..1 float，`<1.20.6` 直写、`>=1.20.6` ×255 转 int，边界照搬 `BedrockParticleRenderer.vertex` 的 `//?` 切分）。

26.1.2 侧：`RenderSetup.builder(RenderPipeline)` PSO 路径，`//?` 整方法切分（同 `BrRenderTypeFactory` D5 模式）。

### D3 molang 不桥接，保留内嵌 wintersky.molang

不为 wintersky.molang 做 eyelib.molang 适配层：

- as-is 保真依赖 molangjs 的 JS 语义（角度制三角、`??` 经 found_unassigned_variable、Allocation 返回 0 等），适配层会在接缝处制造双重失真。
- wintersky 粒子表达式只需要 `query.in_range/all/any/approx_eq` 内建 + 曲线 variableHandler，无实体上下文 query 需求（Blockbench 预览同样没有）。
- **后续可选**：以 `molang_cases.json` 差分用例集跑 eyelib.molang，产出两引擎语义差异清单，再评估统一（列入 P5，非承诺）。

### D4 tick 驱动：渲染帧驱动 + 暂停守卫

wintersky `Emitter.tick(dt)` 内部以固定 1/30s 步长累积消费 `dt`。调用点二选一：

| 方案 | dt 来源 | 与 Blockbench parity | MC 暂停语义 | 结论 |
|---|---|---|---|---|
| **渲染帧驱动**（采用） | RenderTick START，真实帧 dt（秒） | ✅（Blockbench 即 rAF 驱动） | 需 `Minecraft.getInstance().isPaused()` 守卫 | 默认 |
| client tick 驱动 | 固定 0.05s | 视觉颗粒度相同（30Hz 步进）但高帧率下与预览不同步 | 天然正确 | 配置项备选 |

不引入 JE 渲染插值（wintersky as-is 无插值，30Hz 离散步进即参考外观）。

### D5 纹理解析：fetchTexture 钩子 → MC ResourceManager

- `Scene.fetchTexture(path)` 默认实现（bridge 提供）：路径即 Bedrock 纹理路径（如 `textures/particle/particles`），直接映射 `ResourceLocation(namespace, path + ".png")` 走 MC 纹理管理器——vanilla 基岩纹理经资源包路径直读，无需 DynamicTexture。
- `Config.updateTexture` 的 5 张内置纹理已在 `src/main/resources/wintersky/textures/`，映射为 `eyelib:wintersky/textures/*.png`。
- flipbook 需要的图集像素尺寸 [实现期核对：确认 ShaderMaterial 哪个 uniform 依赖图像尺寸]：bridge 加载时用 `NativeImage` 读取 PNG 头/尺寸回填 `TextureImage`。
- `.mcpack` 内纹理：复用现有资源包解包产物（`run/resourcepacks/`），按同一 ResourceLocation 规则命中。

### D6 事件副作用经 Port 出域

`Emitter.runEvent` 的 `sound_effect` / 外部副作用不允许在 wintersky 模块内触 MC：

- 新增 domain 侧接口 `eyelib.wintersky.WinterskyEventPort`（`playSound(String event)` 等最小面），bridge 实现播 MC 音效。
- `particle_effect` 子发射器（emitter_bound / particle_with_velocity）已在模块内闭环（经 `Scene.fetchParticleFile` 钩子 + `child_configs` 缓存），不出域。
- `expression` 事件模块内消化。

---

## 3. 集成架构

### 3.1 新增文件清单

| 文件 | 层 | 职责 |
|------|-----|------|
| `bridge/particle/adapter/WinterskySceneManager.java` | ACL | 持有 `Scene` 单例；事件挂钩（RenderTick/AFTER_ENTITIES/LoggingOut）；spawn/despawn 入口；暂停守卫 |
| `bridge/particle/adapter/WinterskyParticleRenderer.java` | ACL | 遍历 scene→emitters→particles，PoseStack + `//?` vertex 写 quad |
| `bridge/particle/adapter/WinterskyRenderTypes.java` | ACL | materialType×纹理 → RenderType/RenderSetup 缓存（`//?` 26.1 切分） |
| `bridge/particle/adapter/WinterskyTextureLoader.java` | ACL | fetchTexture 默认实现 + NativeImage 尺寸回填 |
| `bridge/particle/adapter/WinterskyParticleFileLoader.java` | ACL | fetchParticleFile 默认实现：identifier → JSON 文本（资源包/.mcpack 解包目录） |
| `eyelib/wintersky/WinterskyEventPort.java` | Domain（wintersky 模块内） | 音效等副作用 Port（D6） |

包依赖方向：`bridge.particle.adapter → eyelib.wintersky`（单向，符合 ADR-0016 bridge→Domain）；wintersky 模块保持零 MC import。

### 3.2 渲染时序（每帧）

```
RenderTick START:
    if (!Minecraft.isPaused()) scene.tick(frameDtSeconds)     // D4

AFTER_ENTITIES (<26.1):
    scene.updateFacingRotation(cameraProxy)                   // camera: MC main camera 位置/旋转 → wintersky Camera 替身
    scene 图根 updateMatrixWorld(true)                        // 等价 three WebGLRenderer 每帧行为 [实现期核对调用点]
    for emitter in scene.emitters:
        if (!emitter.started()) continue
        RenderType rt = WinterskyRenderTypes.get(emitter.material(), texture)
        VertexConsumer vc = bufferSource.getBuffer(rt)
        for particle in emitter.particles:
            poseStack.pushPose()
            poseStack.last().pose().mul(emitterSpaceTransform)   // 世界/发射器空间，同 BedrockParticleRenderer 的 space 处理
            poseStack.mulPoseMatrix(particle.mesh().matrixWorld().toJoml())
            writeQuad(vc, particle.geometry().uv(), particle.geometry().clr())
            poseStack.popPose()
```

- 顶点局部坐标：PlaneGeometry 2×2 → `(±1, ±1, 0)`，由 matrixWorld 完成全部位移/旋转/缩放（含 flipbook 与颜色已写入 attribute，无 CPU 重算）。
- 法线：固定 `(0,0,1)` 经 normal 矩阵旋转（光照恒 FULL_BRIGHT，法线实际不影响输出，写与现有粒子一致即可）。

### 3.3 26.1.2 路径

- 事件挂钩 `//?` 照搬 `ParticleRenderHooks` 现状（`<26.1` AFTER_ENTITIES；26.1 侧 submit/阶段事件按届时 RenderSink 模式接）。
- RenderType → RenderSetup/RenderPipeline `//?` 整方法切分（D2 表内注明）。
- `vertex()` 14 参/链式 `//?` 照搬。
- 归类：全部 L1/L2 机械差异，无新范式断裂。

---

## 4. 规格（前置/后置/不变量/异常/副作用）

**前置条件**：
- F1 `WinterskySceneManager` 初始化晚于 MC 资源管理器可用（纹理/JSON 直读依赖 ResourceManager）。
- F2 `Scene.fetchTexture` / `fetchParticleFile` 钩子在首次 spawn 前完成注入（未注入时 `hasFetchParticleFile()` 为 false，子发射器事件静默跳过——wintersky as-is 行为）。

**后置条件**：
- B1 同一 Bedrock 粒子 JSON + 同一种子下，eyelib 内 wintersky 运行时的逐 tick 粒子状态与 Node oracle 一致（已由 `WinterskyOracleTest` 覆盖，集成后保持绿）。
- B2 渲染输出与 Blockbench/Snowstorm 预览在材质语义上等价（blend/裁剪/双面/无光照）。

**不变量**：
- I1 `io.github.tt432.eyelib.wintersky..` 零 `net.minecraft.*` / `net.minecraftforge.*` / `net.neoforged.*` import（集成后纳入 ArchUnit，见 §6.1）。
- I2 wintersky 模块不被 `eyelib.particle` 依赖；两运行时仅可在测试/编排层相遇。
- I3 MC 侧代码不修改 wintersky 模块内任何文件；行为修正一律回到 ADR-0034 的偏离清单流程。
- I4 纹理/JSON 解析失败不抛到渲染线程：fetch 钩子返回 null → wintersky as-is 回退（内置 missing 纹理 / 跳过子发射器）。

**异常行为**：
- E1 资源缺失：`fetchTexture` 返回 null → Config.updateTexture 走内置 `missing` 占位图（as-is）。
- E2 粒子 JSON 非法：Gson 解析异常在 spawn 边界捕获，记日志，不创建 emitter。
- E3 暂停：暂停期间 `scene.tick` 不调用，粒子冻结；渲染仍绘制最后状态（与 MC 粒子一致）。

**副作用**：
- S1 音效事件经 `WinterskyEventPort` 播 MC 音效（bridge 实现）。
- S2 子发射器经 `Scene` 内部递归创建，占用全局 `max_emitter_particles=30000` 配额（as-is，全场景共享上限）。
- S3 LoggingOut 时 `WinterskySceneManager` 清空 Scene（含所有 emitter 的 mesh/材质缓存）。

---

## 5. 分期计划

| 期 | 内容 | 验收 |
|----|------|------|
| **P1** 渲染最小闭环（1.20.1） | WinterskySceneManager + Renderer + RenderTypes + 硬编码纹理路径；ArchUnit 纳入 wintersky 包 | 游戏内 spawn `zz_branches` + 3 个 vanilla 夹具，AFTER_ENTITIES 可见；RenderDoc 截帧确认 blend/裁剪正确 |
| **P2** 资源与生命周期 | fetchParticleFile/fetchTexture 默认实现、sound Port、spawn API、LoggingOut 清理、暂停守卫 | .mcpack 内粒子文件可加载；音效事件可闻；切世界无泄漏 |
| **P3** 多版本 | 1.21.1 编译；26.1.2 `//?` 切分（RenderSetup/事件/vertex） | 三 node 编译；26.1.2 client 启动 |
| **P4** 交叉校验 harness | 同 JSON 双运行时统计对比（粒子数、寿命分布、位置均值/方差） | 产出 eyelib.particle 差异清单文档 |
| **P5**（可选，非承诺） | molang 双引擎差分评估；游戏内预览 UI 工具化 | 评估报告 |

---

## 6. 验证策略

### 6.1 G1 ArchUnit
`ArchitectureTest.DOMAIN_CLASSES` 谓词加入 `io.github.tt432.eyelib.wintersky..`（当前未覆盖，移植期靠自律保持零 MC）。[实现期核对：ADR-0002 依赖白名单是否需显式登记 `bridge.particle → wintersky`]

### 6.2 G2 spec/oracle
- 已有：`WinterskyOracleTest`（14 用例×45 tick 逐值比对）保持绿，作为回归底线。
- 新增：RenderTypes 映射单测（materialType→blend/depthWrite/cull 断言，MC 依赖部分放 bridge 测试或 clientsmoke）。

### 6.3 G3 视觉/集成
- clientsmoke：spawn 夹具截帧，与 Snowstorm 导出截图人工/半自动对比。
- RenderDoc：确认透明粒子 depthWrite off、additive 混合、无多余状态泄漏（材质调试遵守 `mc-client-visual-debug` / `framebuffer-visual-verification` 流程）。

---

## 7. 风险登记

| # | 风险 | 缓解 |
|---|------|------|
| R1 | 高刷屏 dt < 1/30s 时部分帧 0 步进，视觉微顿 | 与 Blockbench 一致（as-is），不缓解；文档化 |
| R2 | 30k 全局粒子上限与 MC 粒子/实体渲染预算叠加 | 文档化；spawn API 暴露当前用量 |
| R3 | 双运行时同屏渲染顺序未定义（均 AFTER_ENTITIES，透明无排序） | as-is 不排序（wintersky 亦无排序）；如现实际问题再议 |
| R4 | flipbook 图集尺寸 uniform 依赖 PNG 尺寸读取 | D5 NativeImage 回填；P1 截帧验证 |
| R5 | wintersky.molang 与 eyelib.molang 语义漂移长期并存 | D3 差分用例集；差异清单文档化 |
| R6 | distance 事件定义被解析但 wintersky tick 中不触发（as-is 缺陷） | 不修复（保真）；差异清单注明，eyelib.particle 自行实现 |
