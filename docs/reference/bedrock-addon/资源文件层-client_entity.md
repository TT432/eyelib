# Bedrock Add-On 资源文件层：client_entity

## 1. 适用范围

本页讨论 Resource Pack 中的 client entity 文件，也就是 `minecraft:client_entity` 这类客户端描述文件。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `entity/` | RP 内容层 | 目录族 | stable | O3, O7-CEINTRO, C5 | Resource Pack 中的标准入口目录 |
| `*.entity.json` | RP 内容层 | 文件 | sample-backed | R7, R3 | 样例常见命名形式，不应单独上升为稳定命名规范 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示 client entity 文档版本 | example/reference hybrid | O7-CEINTRO | 官方在 examples 文档中给出 |
| `minecraft:client_entity` | 顶层对象 | 容纳客户端实体描述 | example/reference hybrid | O7-CEINTRO | 这是规范对象名 |
| `description` | 主体对象下的核心区段 | 定义标识、贴图、材质、几何体等映射 | example/reference hybrid | O7-CEINTRO, C5 | 最核心区段 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `description.identifier` | 客户端实体标识 | example/reference hybrid | O7-CEINTRO, C5 | 核心字段 |
| `description.min_engine_version` | 最低引擎版本 | example/reference hybrid | O7-CEINTRO | 可选/兼容性相关，不应写成所有文件都必备 |
| `description.materials` | 材质短名映射 | example/reference hybrid | O7-CEINTRO, C5 | 常与 render controller / geometry 配套 |
| `description.textures` | 纹理短名映射 | example/reference hybrid | O7-CEINTRO, C5 | 常见核心区段 |
| `description.geometry` | 几何体短名映射 | example/reference hybrid | O7-CEINTRO, C5 | 常见核心区段 |
| `description.render_controllers` | 关联 render controller | example/reference hybrid | O7-CEINTRO, C5 | 这里填的是 controller 名称，不是文件路径 |
| `description.animations` | 动画短名映射 | example/reference hybrid | O7-CEINTRO, C5 | 常见核心区段 |
| `description.animation_controllers` | 关联 animation controller | example/reference hybrid | O7-CEINTRO | 常见核心区段 |
| `description.scripts` | Molang 脚本钩子，如 `pre_animation`、`scale` | example/reference hybrid | O7-CEINTRO, C5 | 可选增强区段 |
| `description.locators` | locator 名称到定位信息的映射 | example/reference hybrid | O7-CEINTRO | 属于增强区段，不是每个样例都出现 |
| `description.enable_attachables` | 控制 attachable 能否挂接 | example/reference hybrid | O7-CEINTRO | 可选增强字段 |
| `description.held_item_ignores_lighting` | 手持物受光照的特殊处理 | example/reference hybrid | O7-CEINTRO | 客户端渲染增强字段 |
| `description.hide_armor` | 隐藏护甲显示 | example/reference hybrid | O7-CEINTRO | 客户端显示增强字段 |
| `description.sound_effects` | 声音短名映射 | example/reference hybrid / sample-backed | O7-CEINTRO, R7, I3 | 这是短名别名，不是 `sounds.json` 文件路径 |
| `description.particle_effects` | 粒子短名映射 | example/reference hybrid / sample-backed | O7-CEINTRO, R7, I3 | 这是短名别名，不是 `particles/` 文件路径 |
| `description.spawn_egg` | 刷怪蛋显示相关字段 | example/reference hybrid | O7-CEINTRO | 可选增强区段 |

## 5. 写作边界

1. `client_entity` 的主要官方页仍然更接近 **example/reference hybrid**；要下钻到字段合法性和嵌套边界，优先再对照 `Mojang/bedrock-schemas` 的 RP schema（见 `来源清单.md` 中 S4）。
2. `sound_effects`、`particle_effects`、`animations`、`render_controllers` 本质上都是**名称映射或名称引用**，不是文件路径。
3. `spawn_egg.texture` / `spawn_egg.texture_index` 更接近纹理索引体系的交叉点，写法上应和 `texture_indices` 页互相链接，不要在本页孤立解释成一般纹理路径。
4. 仓内 importer 当前显式 codec（I3）已经覆盖 `min_engine_version`、`materials`、`textures`、`geometry`、`animations`、`animation_controllers`、`particle_effects`、`sound_effects`、`render_controllers`、`scripts`、`spawn_egg`；但像 `locators`、`enable_attachables`、`held_item_ignores_lighting`、`hide_armor` 等增强字段仍不应写成“仓内已完全 typed 建模”。

## 6. 主要来源编号

- S4
- O3
- O7-CEINTRO
- C5
- I3
- R3
- R7
