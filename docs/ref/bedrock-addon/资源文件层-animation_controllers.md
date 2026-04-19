# Bedrock Add-On 资源文件层：animation_controllers

## 1. 适用范围

本页讨论 `animation_controllers/` 目录下的动画控制器文件，也就是以状态机形式控制动画、粒子、声音触发的 JSON 文件。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `animation_controllers/` | RP/BP 内容层 | 目录族 | stable | O8-COMPPACK, O8-ANIMCTRL-INTRO, C9 | RP 和 BP 都会用到 |
| `*.json` | RP/BP 内容层 | 文件 | stable sample | O8-ANIMCTRL-REF, R7, R8 | 样例常见后缀如 `.animation_controllers.json`，但不应写成强制规则 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示 animation controller 文档版本 | stable | O8-ANIMCTRL-REF | 常见如 `1.10.0`、`1.17.30` |
| `animation_controllers` | 顶层对象 | 容纳 controller ID -> controller body 映射 | stable | O8-ANIMCTRL-REF, O8-ANIMCTRL-INTRO | 正式顶层对象 |
| `controller.animation.*` | 对象键 | 单个动画控制器 ID | stable sample | O8-ANIMCTRL-REF, R7 | 常见命名风格 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `initial_state` | 指定初始状态 | stable | O8-ANIMCTRL-INTRO, O8-ANIMCTRL-REF | 常见核心字段 |
| `states` | 定义状态集合 | stable | O8-ANIMCTRL-REF | 最核心区段 |
| `states.<name>.animations` | 当前状态播放的动画列表 | stable | O8-ANIMCTRL-REF, C10 | 核心区段 |
| `states.<name>.transitions` | 状态跳转条件 | stable | O8-ANIMCTRL-REF, C10 | 核心区段 |
| `states.<name>.variables` | 状态内变量 | stable | O8-ANIMCTRL-REF | 可选增强区段 |
| `states.<name>.blend_transition` | 状态切换混合时间 | stable | O8-ANIMCTRL-INTRO, O8-ANIMCTRL-REF | 常见增强字段 |
| `states.<name>.sound_effects` | 状态触发声音 | sample/reference hybrid | O8-ANIMCTRL-REF, R4 | 依赖短名映射 |
| `states.<name>.particle_effects` | 状态触发粒子 | sample/reference hybrid | O8-ANIMCTRL-REF, C10 | 依赖短名映射 |

## 5. 写作边界

1. Animation controller 是**状态机文件**，不等于 animations 文件本体。
2. `animations` 区段里放的是短名或带条件的动画调用，不是动画定义本身。
3. 粒子和声音触发可以写，但要明确它们依赖其他资源族里的短名映射和文件定义。

## 6. 主要来源编号

- O8-COMPPACK
- O8-ANIMCTRL-INTRO
- O8-ANIMCTRL-REF
- C9
- C10
- R4
- R7
- R8
