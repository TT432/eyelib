# Bedrock Add-On 资源文件层：particles

## 1. 适用范围

本页讨论 Resource Pack 中的粒子文件，也就是 `particle_effect` 文档结构。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `particles/` | RP 内容层 | 目录族 | stable | R7, O7-PARTICLE-INTRO, C6 | 粒子文件标准目录 |
| `*.json` | RP 内容层 | 文件 | stable sample | R7, R3, R4 | 常见粒子文件形式 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示粒子文档版本 | stable | O7-PARTICLE-DOC | 官方粒子文档明确出现 |
| `particle_effect` | 顶层对象 | 容纳粒子定义主体 | stable | O7-PARTICLE-DOC | 正式顶层对象 |
| `description` | `particle_effect` 下区段 | 定义粒子标识与基础渲染参数 | stable | O7-PARTICLE-DOC | 最核心区段 |
| `components` | `particle_effect` 下区段 | 定义 emitter / motion / appearance 等行为 | stable | O7-PARTICLE-DOC, C6 | 核心区段 |
| `events` | `particle_effect` 下区段 | 定义事件驱动行为 | stable | O7-PARTICLE-DOC | 可选扩展区段 |
| `curves` | `particle_effect` 下区段 | 定义可复用曲线 | stable | O7-PARTICLE-DOC | 可选扩展区段 |

## 4. 主要字段/区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `description.identifier` | 粒子 ID | stable | O7-PARTICLE-DOC, C6 | 核心字段 |
| `description.basic_render_parameters.material` | 使用的粒子材质 | stable | O7-PARTICLE-DOC | 常见值如 `particles_alpha` |
| `description.basic_render_parameters.texture` | 使用的粒子纹理 | stable | O7-PARTICLE-DOC | 指向纹理资源 |
| `minecraft:emitter_rate_manual` | 手动发射率组件 | tutorial/sample-backed | O7-PARTICLE-INTRO, C6 | 应标注 legacy-ish/manual，用途存在但不是默认现代模板 |
| `minecraft:particle_appearance_billboard` | billboard 外观组件 | stable | O7-PARTICLE-DOC | 常见外观组件 |
| `texture_width` / `texture_height` | 纹理尺寸元数据 | stable | O7-PARTICLE-DOC | 可选元数据，不是每个粒子都必备 |
| `flipbook` | 贴图帧动画 | stable | O7-PARTICLE-DOC | 进阶特性 |
| `minecraft:particle_motion_collision` | 粒子碰撞 | stable | O7-PARTICLE-DOC | 进阶特性 |
| `minecraft:particle_motion_parametric` | 参数化运动 | stable | O7-PARTICLE-DOC | 进阶特性 |
| `minecraft:particle_appearance_lighting` | 外观受光照 | stable | O7-PARTICLE-DOC | 进阶特性 |

## 5. 写作边界

1. 粒子文档可以写成“接近正式参考”，因为官方粒子文档提供了明确顶层结构。
2. `minecraft:emitter_rate_manual` 要写成“文档存在、样例常见，但带有 legacy/manual 语义”，不要把它写成默认模板。
3. `texture_width` / `texture_height` 应写成“可选元数据”，而不是统一必备字段。

## 6. 主要来源编号

- O7-PARTICLE-DOC
- O7-PARTICLE-INTRO
- C6
- R3
- R4
- R7
