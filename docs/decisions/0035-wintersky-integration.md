# ADR-0035: Wintersky 融入 eyelib 的集成架构决策

> 状态：Accepted (2026-09-29)
> 设计规格全文：[../concepts/wintersky-integration.md](../concepts/wintersky-integration.md)
> 前置：ADR-0034（as-is 移植）、ADR-0002/0014/0015/0016/0017（模块与多版本契约）

## 背景

wintersky 1.3.3 逐字移植完成（`io.github.tt432.eyelib.wintersky`，零 MC，Node oracle 逐 tick 对齐）。需要决定它以什么形态接入 eyelib 的客户端渲染与资源管线。

## 决策

| # | 决策 | 要点 |
|---|------|------|
| D1 | **双运行时共存，不合并** | `eyelib.particle`（组件化）为生产路径；`eyelib.wintersky` 为参考精确路径，用途限定为预览级保真 + 交叉校验 oracle。不互相依赖、不合并。 |
| D2 | **渲染绕过 material 管线** | wintersky 材质模型仅 `materialType × 纹理`，新增 `WinterskyRenderTypes` 直接映射 4 类 RenderType（cutout/solid/translucent/additive），不经 `BrRenderTypeFactory`。恒 FULL_BRIGHT、NoCull。 |
| D3 | **molang 不桥接** | 保留内嵌 wintersky.molang（molangjs 语义是保真的一部分）；与 eyelib.molang 的统一仅以差分用例评估，非承诺。 |
| D4 | **渲染帧驱动 tick + 暂停守卫** | RenderTick START 传真实帧 dt（Blockbench rAF parity）；`isPaused()` 守卫；不引入 JE 插值。 |
| D5 | **fetchTexture → MC ResourceManager** | Bedrock 纹理路径直接映射 ResourceLocation；flipbook 图集尺寸用 NativeImage 回填。 |
| D6 | **副作用经 Port 出域** | `sound_effect` 等经 `WinterskyEventPort`（domain 定义，bridge 实现）；子发射器模块内闭环。 |

## 关键不变量

- I1 `eyelib.wintersky..` 零 MC import（纳入 ArchUnit DOMAIN_CLASSES，当前靠自律）。
- I2 wintersky 模块不被 `eyelib.particle` 依赖。
- I3 MC 侧不修改 wintersky 模块文件；行为修正走 ADR-0034 偏离清单流程。
- I4 资源缺失不抛到渲染线程，走 as-is 回退（missing 纹理 / 跳过子发射器）。

## 理由

wintersky 的价值是"未经重新设计的原样语义"（含 JS 怪癖），这既是预览保真的来源，也是它能充当 `eyelib.particle` 校验 oracle 的原因。任何将其并入组件管线或统一 molang 引擎的尝试都会在接缝处双重失真，同时摧毁 oracle 属性。

## 后果

- 代码库长期持有两套粒子运行时与两套 Molang 引擎（已文档化，见设计文档 §2/R5）。
- 集成实现分期 P1（1.20.1 渲染闭环）→ P2（资源/生命周期）→ P3（多版本）→ P4（交叉校验 harness）→ P5（可选评估）。
- distance 事件在 wintersky 中解析但不触发属 as-is 缺陷，不修复（R6）。

## 验证

- G1：ArchUnit 谓词扩展（P1）。
- G2：`WinterskyOracleTest` 保持绿 + RenderTypes 映射单测。
- G3：clientsmoke 截帧对比 Snowstorm 预览；RenderDoc 验证混合状态。
