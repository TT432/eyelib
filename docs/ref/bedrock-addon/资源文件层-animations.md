# Bedrock Add-On 资源文件层：animations

## 1. 适用范围

本页讨论 Resource Pack 中的动画文件，也就是 `animations/` 目录下以 `animations` 顶层对象组织的动画数据。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `animations/` | RP 内容层 | 目录族 | stable | O8-COMPPACK, O8-ANIM-OV, C9 | 动画文件的标准目录 |
| `*.json` | RP 内容层 | 文件 | stable sample | O8-ANIM-OV, R7 | 样例常见命名如 `*.animation.json`，但后缀不应写成强制规则 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示动画文档版本 | stable | O8-ANIM-OV, C11 | 常见如 `1.8.0` |
| `animations` | 顶层对象 | 容纳动画 ID -> 动画体映射 | stable | O8-ANIM-OV, C11 | 正式顶层对象 |
| `animation.*` | 对象键 | 单个动画 ID | stable sample | O8-ANIM-OV, R7 | 常见命名风格，不是语法关键字 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `loop` | 控制动画是否循环 | stable | O8-ANIM-OV, C11 | 常见基础字段 |
| `animation_length` | 指定动画长度 | stable | O8-ANIM-OV | 非所有动画都必需 |
| `bones` | 以骨骼名组织动画通道 | stable | O8-ANIM-OV, O8-ENTITYMODEL | 最核心区段 |
| `rotation` | 旋转通道 | stable | O8-ANIM-OV, C11 | 常见基础通道 |
| `position` / `translation` | 位移通道 | stable | O8-ANIM-OV, C11 | 常见基础通道 |
| `scale` | 缩放通道 | stable | O8-ANIM-OV, C11 | 常见基础通道 |
| `sound_effects` / `particle_effects`（与动画事件联动） | 触发声音或粒子 | tutorial/sample-backed | O8-ENTITYMODEL, R4 | 需要配合 client entity 短名映射理解 |

## 5. 写作边界

1. `client_entity.description.animations` 是**短名映射层**，不是动画文件本体；动画文件本体在 `animations/`。
2. 文件后缀如 `.animation.json`、`.a.json` 只能写成样例惯例，不能写成官方强制命名规则。
3. 核心语义应集中在 `format_version`、`animations`、`bones` 和各动画通道上。

## 6. 主要来源编号

- O8-COMPPACK
- O8-ANIM-OV
- O8-ENTITYMODEL
- C9
- C11
- R4
- R7
