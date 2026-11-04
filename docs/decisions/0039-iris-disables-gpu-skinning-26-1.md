# ADR-0039：Iris 光影下 26.1 GPU 蒙皮必须回退 CPU 路径

## 状态

已接受（2026-11-04）。修复 Iris 1.11.4 + 26.1.2 光影开启后天空整片发黑、模型发白错位。

## 背景

症状（生产实例 `work/prod/26.1.2`：NeoForge 26.1.2.126 + eyelib 21.1.26 + dokimobs 0.3.1 + Iris 1.11.4 + Sodium 0.9.2 + MakeUp-UltraFast 9.6）：

- 开启光影后，画面**上半部整片黑**，天空只剩一道巨大斜面，云与地形消失；
- doki 模型发白、几何错位，但残留可辨认；
- 关闭光影（`iris.properties` `enableShaders=false`）一切正常；
- 移除 eyelib+dokimobs（仅 Iris+Sodium+光影包）一切正常 → 与本模组偶发；
- 保留模组但 `dokimobs-common.toml` `spawn_chance=0.0`（不渲染任何 doki 模型）也正常 → 由**模型渲染**触发。

### 根因（实证）

26.1 的 GPU 蒙皮（`NgSkinningManager`）从 routing `RenderType` 派生**自有 RenderPipeline**：

```java
routing.toBuilder()
    .withVertexShader(Identifier.parse("eyelib:core/entity_skinned"))
    .withVertexFormat(SkinnedGeometry.FORMAT, VertexFormat.Mode.TRIANGLES)
    .withUniform("BonePalette", UniformType.UNIFORM_BUFFER)
```

Iris 26.1 改为**按 `RenderPipeline` 对象身份**分发光影程序（`IrisPipelines.coreShaderMap`）：只有 vanilla `RenderPipelines.*` 常量与经 `IrisApi.assignPipeline` 登记的 pipeline 会被换成光影包程序，其余走原版程序分支。蒙皮管线携带自有顶点格式（`TRIANGLES` + BonePalette 布局），原版程序按 vanilla 实体格式解释这些顶点 → 顶点属性整体错位，画出铺满屏幕的巨大三角面并污染同批次的天空/云渲染。

1.21.1 不受影响：`LegacySkinningManager.createSession` 早在影激活时返回 null（Oculus/Iris 锁深度/颜色写入的实证回退，见 ADR-0032）；26.1 的 `NgSkinningManager` 漏了同款判定。

## 决策

1. `NgSkinningManager.createSession` 在 `OculusCompat.shaderPackActive()`（`IrisApi.isShaderPackInUse()`）为 true 时返回 null，回退经典 CPU 蒙皮；提示只打一次。
2. 自定义材质 `RenderPipeline`（`BrRenderTypeFactory.buildPipeline`，26.1）构建后用 `IrisApi.assignPipeline` / `assignPipelineShadow` 登记主通道与阴影通道程序（`IrisPipelineCompat`，反射调用、Iris 缺席时静默跳过），使 CPU 路径的自定义 RenderType 也参与 G-buffer。

## 后果

- 光影开启期间 GPU 蒙皮不可用：性能回退到 CPU 蒙皮（与 1.21.1 现状一致）。
- 需要真正的光影 + GPU 蒙皮共存时，须由 Iris 侧支持第三方管线（或干脆不做自有着色器），不在本模组可控范围。
- `-Deyelib.gpuSkinning=false` 仍是全局开关；光影回退与之叠加，语义一致。

## 验证记录

生产实例 `work/prod/26.1.2`（HMCL headless 启动，`--quickPlaySingleplayer prod_fixture`，PrintWindow 抓帧）：

| 条件 | 结果 | 证据 |
|---|---|---|
| 光影开 + 现有构建 | 天空整片黑、巨大斜面、模型发白 | `work/prod/shots/prod262/sunlit.png`、`t3.png` |
| 光影开 + `-Deyelib.gpuSkinning=false` | 正常（天空/云/模型/阴影） | `work/prod/shots/prod262/noskin.png` |
| 光影开 + 强制 vanilla RenderType（自定义管线绕过） | 仍黑 → 排除自定义 RenderType | `work/prod/shots/prod262/forcervanilla.png` |
| 光影关（回归对照） | 正常 | `work/prod/shots/prod262/noshader.png` |
| 移除 eyelib+dokimobs（隔离对照） | 正常 | `work/prod/shots/prod262/iris_only.png` |
| `spawn_chance=0`（不渲染模型） | 正常 | `work/prod/shots/prod262/spawn0.png` |
| **修复后（默认配置，光影开）** | 正常；日志 `[skinning] 检测到 Oculus/Iris 光影激活，GPU 蒙皮回退经典 CPU 路径` | `work/prod/shots/prod262/fixed_shaders.png` |

## 未覆盖

- 1.20.1 + Oculus 实机（本机无该实例）。
- 1.21.1 侧无改动需求：`LegacySkinningManager` 已有同款回退，本次改动位于 `//? if >=26.1` 分支。
