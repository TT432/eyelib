# Bedrock Add-On 资源文件层：attachables

## 1. 适用范围

本页讨论 Resource Pack 中的 attachable 文件，也就是 `minecraft:attachable` 这一类资源。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `attachables/` | RP 内容层 | 目录族 | stable | O8-ATTACH, O8-COMPPACK, C12 | attachable 主目录 |
| `*.json` | RP 内容层 | 文件 | stable sample | O8-ATTACH, O8-ITEMS | 样例常见命名形式，如 `*.player.json`、`*.attachable.json` |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示 attachable 文档版本 | stable | O8-ATTACH | 常见如 `1.20.30` |
| `minecraft:attachable` | 顶层对象 | 容纳 attachable 定义 | stable | O8-ATTACH | 正式顶层对象 |
| `description` | attachable 主体区段 | 定义标识、item 条件、贴图、材质、几何体等 | stable | O8-ATTACH | 最核心区段 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `description.identifier` | attachable 标识 | stable | O8-ATTACH | 核心字段 |
| `description.item` | item 到 attachable 的条件映射 | stable | O8-ATTACH | attachable 关键区段 |
| `description.materials` | 材质映射 | stable | O8-ATTACH | 与 entity 视觉语义相似 |
| `description.textures` | 纹理映射 | stable | O8-ATTACH | 与 entity 视觉语义相似 |
| `description.geometry` | 几何体映射 | stable | O8-ATTACH | 与 geometry 资源配套 |
| `description.animations` | attachable 可用动画映射 | stable | O8-ATTACH | 与 animations 配套 |
| `description.scripts.animate` | 控制 attachable 动画播放 | stable | O8-ATTACH | 常见于第一/第三人称切换 |
| `description.render_controllers` | 指定 render controller | stable | O8-ATTACH | 常见区段 |
| `binding`（geometry 侧） | 决定挂接位置 | tutorial/sample-backed | O8-ATTACH, C12, C13 | 常见于 attachable geometry 配置 |

## 5. 写作边界

1. Attachable 是独立资源族，不应直接并入 client entity 页里一笔带过。
2. `*.player.json` 这类命名只能写成样例命名习惯，不应写成固定规则。
3. Attachables 与 geometry / animations / render_controllers 的关联要写清楚，但不能因此把它们混成一个文件族。

## 6. 主要来源编号

- O8-ATTACH
- O8-ITEMS
- O8-COMPPACK
- C12
- C13
