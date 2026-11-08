# ADR-0040：实体材质默认光照模型改为 LIGHTMAP_ONLY——26.1 方向光根修（取代 ADR-0033 的实体默认值）

## 状态

已接受（2026-11-04）。修订 ADR-0033 的实体默认值部分（粒子的 AMBIENT/NONE 语义不变）。
2026-11-08 修订决策 2：版本分叉从 domain（`BrRenderStateFactory`）下移到 ACL
（`RenderTypeCapabilities`），行为不变，`//?` 闸门基线因此归零。

## 背景

症状（dokimobs 消费方实机，26.1.2）：Bedrock 实体模型表面出现成片暗块（棕/粉色纹理被压成暗紫红），
且**骨骼动画播放时整块面亮度在深/浅之间来回跳变**——用户描述为"阴影闪烁"。同资产在 1.21.1 上观感正常。

已验证根因链：

1. **26.1 起 vanilla `entity.vsh` 默认叠加方向光**：
   ```glsl
   #elif defined(NO_CARDINAL_LIGHTING)
       vertexColor = Color;
   #else
       vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color);
   #endif
   ```
   `light.glsl` 的 `minecraft_mix_light` = `color.rgb * min(1, (max(0,dot(L0,N)) + max(0,dot(L1,N))) * 0.6 + 0.4)`
   ——**面亮度 ∈ [0.4, 1.0]，由世界空间法线决定**。`RenderPipelines.ENTITY_SOLID/ENTITY_CUTOUT/
   ENTITY_TRANSLUCENT` 全部基于 `ENTITY_SNIPPET`（无该 define），只有 `EYES` 显式加
   `NO_CARDINAL_LIGHTING`，可见默认即开启。
2. **eyelib 的实体材质默认 `LightingModel.DIRECTIONAL`**（`BrRenderStateFactory.compute`）。该值在
   ADR-0033 定下时的含义是"走 vanilla 实体 shader"——1.20.1/1.21.1 的 vanilla entity shader **没有**
   方向项，所以 DIRECTIONAL 实际观感即 lightmap-only。26.1 的 vanilla shader 语义变了，这个状态值
   却仍是 DIRECTIONAL，于是 26.1 的 26.x 分支把 vanilla RenderType 直接当"方向光"用，**语义漂移**。
3. **Bedrock 实体纹理已烘焙面明暗**（A&S/Blockbench 资产的 per-face shade），再乘一层 [0.4,1.0]
   得到 0.4 暗面；骨骼动画改变面法线 → 同一面亮度在 0.4↔1.0 间跳变 = 观察到的闪烁。
4. 1.21.1 无该 define 分支的等价方向项，故同资产观感正常——可作对照。

## 决策

1. `BrRenderState.LightingModel` 新增 **`LIGHTMAP_ONLY`**：仅 lightmap，无方向项。
   - 26.1 的 `buildPipeline` 对它加 `NO_CARDINAL_LIGHTING`（既有条件 `lighting != DIRECTIONAL` 已覆盖）。
   - 与 DIRECTIONAL 的区别只在渲染路径：需要自建 RenderType（走 eyelib 自定义 pipeline）。
2. **实体默认光照模型与版本无关，版本分叉只在 ACL**（2026-11-08 修订，原决策为「`BrRenderStateFactory.compute`
   按版本分叉」）：`BrRenderStateFactory.compute` 恒定产出 `DIRECTIONAL`，语义 = 「走宿主 vanilla 实体
   着色器通道」。26.1 的「不得叠加方向光」由 ACL 施加：
   `bridge.material.adapter.RenderTypeCapabilities.channelState(state)` 在 `>=26.1` 把
   `DIRECTIONAL` 翻成 `LIGHTMAP_ONLY`，`BrRenderTypeFactory.create` 用它算出的有效 state 做门控与建管线。
   理由：
   - `DIRECTIONAL` 与 `LIGHTMAP_ONLY` 的差别**只存在于 26.1 的 pipeline define**；在 `<26.1` 上两者渲染
     等价，仅差一个「vanilla RenderType 名录」判断。
   - 名录与 pipeline define 都是 ACL 知识（`RenderTypeCapabilities.needsCustomRenderType` 与
     `BrRenderTypeFactory.buildPipeline` 已在 bridge）。
   - domain 保留版本分叉会被 `StonecutterCommentPlacementTest`（ADR-0016 §6）记为违规。
2b. **「哪些 (transparency, cull) 组合只能走 custom」同属 ACL**：原 `BrRenderState.needsCustomRenderType()`
   整体迁入 `RenderTypeCapabilities.needsCustomRenderType(state)`，其中 `>=26.1` 保留
   `(BLEND && cull)`、`(NONE && !cull)` 两个 vanilla 名录缺口组合。
2c. 由此 26.1 的实体材质恒获得非 `DIRECTIONAL` 的有效 state ⇒ 全部走自定义 pipeline（与决策 5 的回退路径
   `createForEntityPass` 的 canonical `LIGHTMAP_ONLY` 口径一致）。
3. **粒子路径不变**：新判定 `isParticleUnlit(state)` = `AMBIENT || NONE`，取代原先
   `lighting != DIRECTIONAL` 的三处判定（`<26.1` 的 `shaderState` / `legacySkinningVariant`、
   `IrisPipelineCompat.mainProgram`）。LIGHTMAP_ONLY **不**走 `eyelib:particle_unlit`，仍用 vanilla
   entity shader（含蒙皮变体），只是编译期去掉方向项。
4. Iris 程序名：AMBIENT/NONE 仍统一登记 `ENTITIES`（粒子顶点格式约束）；LIGHTMAP_ONLY 按
   `surfaceClass` 映射（GLINT/EMISSIVE/ENTITIES_TRANSLUCENT/ENTITIES），保持实体的光影语义。
5. **实体渲染主路径必须切到自定义 pipeline**：26.1 的 `RenderPassAdapter.toRenderType` 原先直接返回
   vanilla `RenderTypes.entity*`（不经过 `BrRenderTypeFactory`），而 26.1 的 `ENTITY_CUTOUT` /
   `ENTITY_TRANSLUCENT` 带 `PER_FACE_LIGHTING` define（`entity.vsh` 中优先级**高于**
   `NO_CARDINAL_LIGHTING`，逐面写入前/后两套方向光颜色）。因此仅在 `buildPipeline` 里加 define
   是无效的——主路径根本不走它。改为 `BrRenderTypeFactory.createForEntityPass(texture, transparency,
   disableCulling)`：按 pass 语义构造 LIGHTMAP_ONLY 的 `BrRenderState` 并走 `custom()`
   （`ENTITY_SNIPPET` 不含 `PER_FACE_LIGHTING`）。

## 后果

- 26.1 的实体材质全部走自定义 RenderPipeline（此前不透明/镂空/半透明走 vanilla `RenderTypes`）。
  RenderType 仍按 `(texture, state)` 缓存，同材质多实体共享同一批；Iris 侧由
  `IrisPipelineCompat.assign` 登记主通道与阴影通道，蒙皮派生管线（`NgSkinningManager.derivePipeline`
  的 `toBuilder()`）继承 `NO_CARDINAL_LIGHTING`，经典/蒙皮两条路径观感一致。
- 「是否必须自建 RenderType」的判定在 ACL（`RenderTypeCapabilities.needsCustomRenderType`），
  domain 的 `BrRenderState` 只承载语义分量。
- 光影包激活时按既有回退：GPU 蒙皮回退 CPU 路径，光照由 pack 接管。
- Bedrock 资产的面明暗完全来自纹理与 lightmap，与 1.20.1/1.21.1 一致；不再随动画跳变。

## 验证

- 三节点（1.20.1 / 1.21.1 / 26.1.2）`compileJava` 通过。
- 26.1.2 实机（dev，`doki_smoke26`，村民 + `spawn_chance=1.0`，同角度同实体）A/B：
  - 改动生效证据：`RenderPassAdapter.toRenderType(...)` 返回 `eyelib_material_cutout_*`，其 pipeline 为
    `ShaderDefines[values={ALPHA_CUTOUT=0.1}, flags=[NO_CARDINAL_LIGHTING]]`，不含 `PER_FACE_LIGHTING`。
  - 亮度实测（eyelib 21.1.26 → 21.1.27，同机位）：

    | 区域 | 均值 | 最暗 5% 分位 |
    |---|---|---|
    | 大衣 | 82.6 → 103.0（+25%） | 34 → 47 |
    | 脸 | 123.2 → 170.5（+38%） | 22 → 29 |
    | 头发 | 175.1 → 194.1（+11%） | 159 → 172 |

  - 视觉：改动前面向背光侧的头发/肩部/大衣出现成片暗块，改动后同角度亮度均匀。
- 附带修复：dokimobs 的 `pack.mcmeta` 原只写 `min_format/max_format`，26.1 直接
  `JsonParseException: ... require a supported_formats field`，整个资源包被拒（dev 环境因此看不到任何
  BB 实体）；改为 `supported_formats: {min_inclusive, max_inclusive: 81}` 后模型正常加载。
