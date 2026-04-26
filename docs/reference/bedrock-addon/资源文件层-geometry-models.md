# Bedrock Add-On 资源文件层：geometry / models

## 1. 适用范围

本页把几何体与模型文件放在同一页，先作为一个伞页处理，避免在证据还不够细时过早拆成过多页面。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `models/entity/` | RP 内容层 | 目录族 | stable | O3, O8-ENTITYMODEL, R7 | 实体几何/模型主目录 |
| `models/blocks/` | RP 内容层 | 目录族 | stable | O3, O8-BLOCKS, C14 | 方块模型目录 |
| `*.geo.json` | RP 内容层 | 文件 | stable sample | O8-ENTITYMODEL, R7 | 样例常见命名形式 |
| `mobs.json` | RP 内容层 | 文件 | stable sample | R7 | Vanilla-style 相关文件，更多是样本库上下文 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示 geometry 文档版本 | stable | O8-ENTITYMODEL | 常见如 `1.16.0` |
| `minecraft:geometry` | 顶层对象 | 容纳 geometry 列表 | stable | O8-ENTITYMODEL | 正式顶层对象 |
| `description.identifier` | geometry 描述字段 | 几何体标识 | stable | O8-ENTITYMODEL | 与 client entity / attachable 里的 geometry 映射配套 |
| `bones` | geometry 主体区段 | 定义骨骼层级与挂接关系 | stable | O8-ENTITYMODEL, C13 | 核心区段 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `texture_width` / `texture_height` | 贴图尺寸元数据 | stable | O8-ENTITYMODEL | 常见基础字段 |
| `visible_bounds_width` / `visible_bounds_height` / `visible_bounds_offset` | 可见边界 | stable | O8-ENTITYMODEL | 常见基础字段 |
| `bones[].name` | 骨骼名 | stable | O8-ENTITYMODEL | 常与动画骨骼名匹配 |
| `bones[].parent` | 骨骼父子关系 | stable | O8-ENTITYMODEL | 常见结构字段 |
| `bones[].pivot` | 枢轴点 | stable | O8-ENTITYMODEL | 常见结构字段 |
| `bones[].binding` | 绑定目标 | tutorial/sample-backed | O8-ATTACH, C13 | attachable / player geometry 场景尤其重要 |
| `bones[].cubes` | 模型立方体定义 | stable | O8-ENTITYMODEL | 最核心区段之一 |

## 5. 写作边界

1. 现阶段先把 `geometry / models` 放一页，避免过早把 block/entity 几何拆太碎。
2. `binding` 要写成“某些场景中的关键字段”，尤其 attachables，不应写成每个 geometry 文件都必备。
3. `models/blocks/` 与 `models/entity/` 应明确是两个方向，但暂时共享一个伞页即可。

## 6. 主要来源编号

- O3
- O8-ENTITYMODEL
- O8-ATTACH
- O8-BLOCKS
- C13
- C14
- R7
