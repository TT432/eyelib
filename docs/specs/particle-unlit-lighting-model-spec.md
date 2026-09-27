# 粒子 unlit 光照模型规格

> 决策依据：[ADR-0033 粒子光照模型轴](../decisions/0033-particle-lighting-model-axis.md)。
> 代码权威：`BrRenderState.LightingModel`、`RenderTypeResolver.resolveParticle`、
> `BrRenderTypeFactory.shaderState`。

## 背景

Bedrock 粒子光照语义与 vanilla 实体不同：粒子光照**无方向项**（diffuse 方向光），
只有"受环境光 tint（lit）"与"不受光照（unlit）"两态。vanilla entity shader 的
方向光对两类粒子都是误用，产生**视角相关明暗**（俯视压暗至 ~40%，上游 PR #25
探针实证），违反 Bedrock 语义。

## 定义

`BrRenderState.LightingModel` 三态（`BrRenderState.java:56-66`）：

| 值 | 语义 | 用途 |
|----|------|------|
| `DIRECTIONAL` | lightmap + 法线方向光 | 实体默认 |
| `AMBIENT` | 仅 lightmap tint | Bedrock lit 粒子 |
| `NONE` | 不受光照 | Bedrock unlit 粒子 |

`NONE` 与 `AMBIENT` 实现上共用同一 shader（`eyelib:particle_unlit`），光照差异由
顶点 UV2（lightmap 坐标）承载：unlit 顶点 UV2 置满亮度，lit 顶点取场景光照。

## 映射规则

粒子材质经 `RenderTypeResolver.resolveParticle(materialName, lit)` 解析
（`lit` = 实体描述是否存在 `minecraft:particle_appearance_lighting` 组件，
true → `AMBIENT`，false → `NONE`）：

| 材质 | SurfaceClass | Transparency | cull |
|------|-------------|--------------|------|
| `particles_opaque` / `particles_base` | OPAQUE | NONE | true |
| `particles_alpha` | CUTOUT | ALPHA_TEST | false |
| `particles_blend` | TRANSLUCENT | BLEND | false |
| `particles_add` | `particleAdd()` 派生 | ADDITIVE | — |

全部经 `BrRenderTypeFactory.create` 构造带光照轴的 `BrRenderState`，不再回退
vanilla 逐行复刻路径。

## 不变量

1. **视角无关**：`lighting != DIRECTIONAL` 的粒子任意相机俯仰角下渲染 RGB 不变
   （实机容差：RGB 比值 ≈1.0±0.02）。
2. **lit/unlit 可区分**：同场景 lit 粒子显著暗于 unlit（环境光 tint）。
3. **轴切换保持其余分量**：`withLighting` 仅改光照轴，surface/cull/transparency/
   depth/blend 等不变。
4. **蒙皮不误路由**：`lighting != DIRECTIONAL` 的 state 蒙皮变体登记
   `VARIANT_UNSUPPORTED`（粒子从不走蒙皮，防御性登记）。

## 异常行为（回退链）

1. **shader 注册失败/重载窗口**：`ParticleUnlitShaders.unlitShader()` 为 null 时
   回退 vanilla entity shader（方向光近似，画面退化但不崩）。
2. **Oculus/Iris 光影激活**：`OculusCompat.shaderPackActive()` 为 true 时**不走**
   自定义 `particle_unlit`，回退 vanilla entity shader。理由：非 Iris 自有
   `ShaderInstance.apply()` 被 `DepthColorStorage` 锁深度/颜色写入（零像素，
   ADR-0032 实证）；光影下光照由 pack 接管，vanilla 批次经
   ExtendedShader/FallbackShader 豁免锁定。检测在 ShaderStateShard lambda 内
   每次取值，运行期切换光影包即时生效。
3. **26.1**：unlit 走 vanilla 派生 RenderPipeline（非自定义 ShaderInstance），
   不受上述回退影响；Iris 对 26.1 的接管形态未实证（已知未知项）。

## 副作用

- 新增 shader 资产：`assets/eyelib/shaders/core/particle_unlit.json`（≤1.20.1）与
  `particle_unlit_121.json`（1.21+），fsh 复用 vanilla，vsh 自维护——须与 vanilla
  fog/lightmap/overlay 语义同步维护。
- 光影激活时 unlit 粒子退化为 vanilla 着色（是否出现视角相关 diffuse 由 pack
  决定），退化优于零像素消失。

## 验证

- 单测（`:1.20.1:test`）：`BrRenderStateLightingModelTest` 光照轴契约 4 例；
  `BrRenderStateSpecTest`、`BrMaterialEntryRenderTypeTest` 回归。
- 实机（1.20.1 Forge dev client，midnight 超平坦，详细数据见 ADR-0033 验证节）：
  unlit 粒子俯仰 0°/-45° RGB 视角无关（比值 0.996/1.015/1.000），绝对亮度全亮；
  lit 粒子 ≈(74,74,116)；Oculus 回退补充（2026-09-27）无光影路径回归无损。
- 未覆盖：光影激活分支的实机验证（需 PCL+Oculus+AmberShader 生产实例，
  同 ADR-0032 验证法）；Iris 在 26.1 的接管形态。
