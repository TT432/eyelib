# ADR-0033：渲染状态增加光照模型轴——粒子光照根修（取代 PR #25 局部修补）

## 状态

已接受（2026-09-26）。取代上游 PR #25（TT432/eyelib，"fix(particle): avoid
view-dependent diffuse lighting for unlit alpha particles"，判定为正确但局部的
hotfix，无合并价值，由本 ADR 的系统性修复替代）。

## 背景

症状：Bedrock 粒子（无 `minecraft:particle_appearance_lighting` 组件）亮度随相机
俯仰变化。已验证根因链：

1. 1.20.1 与 1.21.1 的全部 vanilla entity core shader（cutout/translucent/
   translucent_emissive/solid）vsh 均含
   `vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, Color)`；
   billboard 法线随相机旋转 → 视角相关明暗。FULL_BRIGHT lightmap 无法抵消该项。
2. Bedrock 官方语义（creator 文档）：无 lighting 组件 → 全亮；有 lighting 组件 →
   "tinted by local lighting conditions"（环境光 tint，**无方向项**——billboard
   无法线概念）。JE 自家粒子 shader（26.1 `particle.vsh`：
   `Color * sample_lightmap(...)`）同样只有 lightmap、无方向光。
   因此 JE 实体方向光对粒子（lit 与 unlit）都是误用。
3. 结构根因：`PortRenderPass` 只有 `transparency × disableCulling`；
   `BrRenderState.lightmap/overlay` 在 `BrRenderStateFactory` 硬编码 `true`——
   pass 模型**没有任何字段能承载光照语义**，消费方只能在 renderer 里特判。
   粒子桥接同时双层绕行 material 系统：`BedrockParticleRenderer` 用 domain 版
   `resolveParticle`（仅产出规范 pass）并私有复制 `RenderPassAdapter` 的
   pass→RenderType switch；bridge/domain 两份 `resolveParticle` 已发散
   （`particles_add` 一边走 custom 一边走规范 pass）。

## 决策

1. **`BrRenderState` 增加 `LightingModel lighting` 轴**（三态）：
   - `DIRECTIONAL`：lightmap + 法线方向光——实体默认，`BrRenderStateFactory`
     恒产出此值，**实体渲染行为零变化**。
   - `AMBIENT`：仅 lightmap tint，无方向光——有 lighting 组件的粒子。
   - `NONE`：不受光照——无 lighting 组件的粒子；实现上与 AMBIENT 同 shader，
     差异由顶点 UV2 承载（既有 `getLight`：NONE→FULL_BRIGHT，AMBIENT→环境光）。
   - `needsCustomRenderType()` 在 `lighting != DIRECTIONAL` 时为 true。
2. **shader 底座按版本**：
   - ≤1.21.1（legacy RenderType 体系）：自带 core shader `eyelib:particle_unlit`——
     自定义 vsh（`vertexColor = Color`，lightmap/overlay/fog 直通）+ 复用 vanilla
     fragment `minecraft:rendertype_entity_cutout`（与 Forge unlit fsh 逐行同语义，
     含 0.1 alpha 裁剪、overlay mix、lightmap 乘算、linear_fog）。经
     `RegisterShadersEvent` 注册（沿用 LegacySkinningManager 模式），注册失败
     回退 vanilla entity shader（退化回方向光，不崩——2026-08-27 教训）。
     1.21.1 无现成 unlit shader（已验证 neoforge-21.1.248 资源 jar），1.20.1 虽有
     Forge `entity_translucent_unlit`，但两版本统一用自带 shader 保持语义一致。
   - 26.1：entity pipeline 加 `withShaderDefine("NO_CARDINAL_LIGHTING")`
     （vanilla entity.vsh 原生支持的编译期分支）。
3. **解析收敛**：bridge `RenderTypeResolver.resolveParticle(materialName, lit)`
   为粒子材质构造带光照轴的 `BrRenderState` 并统一走 `BrRenderTypeFactory`
   （对齐实体路径与 `particles_add` 既有走法）；删除 domain 版 `resolveParticle`；
   `BedrockParticleRenderer` 删除私有 switch，pass→RenderType 收敛到
   `RenderPassAdapter.toRenderType`。
4. **蒙皮变体**：`lighting != DIRECTIONAL` 的 state 登记
   `VARIANT_UNSUPPORTED`（粒子从不走蒙皮；防御性登记防误路由）。

## 后果

- 粒子在三版本上：unlit 亮度视角无关且全亮；lit 仅受环境光（lightmap），
  消除视角相关 diffuse——对齐 Bedrock 语义与 JE 自家粒子着色模型。
- 粒子 RenderType 从 vanilla 共享实例变为 per-(texture, state) custom 实例
  （`BrRenderTypeFactory.CACHE` 缓存）——batch 粒度略增，可接受。
- `BrRenderState` 新增 record 分量：唯一构造点是 `BrRenderStateFactory.compute`；
  各缓存以 state 值相等性为键，自动正确。
- **已知残留偏差（不在本 ADR 范围）**：实体 emissive 材质仍走 vanilla
  translucent_emissive shader（有 diffuse、无 lightmap）——A&S 蜘蛛红眼修复只
  处理了 lightmap 一半；如需 Bedrock 精确 emissive 语义，后续用同一光照轴
  映射到 NONE/AMBIENT 并补蒙皮变体。
- 新 shader 资产须与 vanilla fog/lightmap/overlay 语义同步维护（fsh 复用 vanilla
  已把该风险降到 vsh 一处）。

## 验证（2026-09-26，1.20.1 Forge dev client）

- 构建：`:1.20.1`/`:1.21.1`/`:26.1.2` compileJava 通过；`:1.20.1:nullawayMain` 通过
  （顺带修复预存门禁阻断：`bridge/client/compat/oculus` 缺 `@NullMarked`，补 package-info）。
- 单测：`:1.20.1:test` 全绿（含新增 `BrRenderStateLightingModelTest` 光照轴契约 4 例）。
- 实机（midnight 超平坦，AIDebugServer 驱动）：`ParticleUnlitShaders.unlitShader() != null`；
  `particles_alpha`/`particles_blend`/`particles_add` 无 lighting 组件粒子在俯仰 0° 与 -45°
  两视角下 RGB 比值 0.996/1.015/1.000（视角无关），绝对亮度 ≈(233-255) 全亮；
  带 lighting 组件粒子 ≈(74,74,116) 显著暗于 unlit（环境光 tint，Bedrock 语义）。
  修复前 vanilla 路径同场景俯视压暗至 ~40%（PR #25 探针数据 (80,220,120)→(32,88,48)）。
