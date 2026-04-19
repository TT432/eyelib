# Bedrock Add-On 资源文件层：sounds

## 1. 适用范围

本页讨论 Resource Pack 里的声音相关文件，重点是：

- `sounds.json`
- `sounds/sound_definitions.json`
- `sounds/**` 下的实际音频文件

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `sounds.json` | RP 内容层 | 文件 | tutorial/reference hybrid | O7-SOUND-INTRO, R7, I4 | 放在 RP 根目录 |
| `sounds/sound_definitions.json` | RP 内容层 | 文件 | tutorial/reference hybrid | O7-SOUND-ADD, R7, I4 | 放在 `sounds/` 目录下 |
| `sounds/**` | RP 内容层 | 文件树 | stable sample | O7-SOUND-ADD, R7 | 存放实际音频资源 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `sounds.json` 根对象 | 顶层对象 | 事件到声音行为的映射 | tutorial/reference hybrid | O7-SOUND-INTRO, C7, I4 | 没有单一包装对象名 |
| `sound_definitions` | `sound_definitions.json` 顶层对象 | 短名到声音资源的映射 | tutorial/reference hybrid | O7-SOUND-ADD, C8, I4 | 这是文档与样例中稳定出现的顶层对象名 |
| `format_version` | `sound_definitions.json` 顶层字段 | 指示定义文件版本 | tutorial/reference hybrid | O7-SOUND-ADD | 常见为 `1.20.20` 等 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `entity_sounds` | `sounds.json` 区段 | 处理实体自动事件声音 | sample/community-backed | C7, R7 | 社区解释与官方样例都常见 |
| `block_sounds` | `sounds.json` 区段 | 处理方块声音映射 | community/importer-backed | C7, I4 | 仓内 parser 已单独分区，但当前页不把它抬升为官方字段表 |
| `interactive_block_sounds` | `sounds.json` 区段 | 处理交互方块声音映射 | community/importer-backed | C7, I4 | 仓内 parser 已单独分区，但当前页不把它抬升为官方字段表 |
| `individual_event_sounds` | `sounds.json` 区段 | 处理事件级声音映射 | tutorial/reference hybrid | O7-SOUND-INTRO, C7 | 不要和 `sound_definitions` 混淆 |
| `sound_definitions.<id>` | `sound_definitions.json` 条目 | 定义一个短名声音 | tutorial/reference hybrid | O7-SOUND-ADD, C8 | 常见主条目 |
| `category` | sound definition 字段 | 控制声音类别 | tutorial/reference hybrid | O7-SOUND-ADD, C8 | 常见核心字段 |
| `sounds` | sound definition 字段 | 列出实际声音资源 | tutorial/reference hybrid | O7-SOUND-ADD, C8 | 可为字符串数组或对象数组语义 |
| `min_distance` / `max_distance` | sound definition 字段 | 距离相关行为 | tutorial/reference hybrid | O7-SOUND-ADD | 可选增强字段 |
| `__use_legacy_max_distance` | sound definition 字段 | 旧兼容相关开关 | tutorial/reference hybrid | O7-SOUND-ADD | 应标注 legacy/compatibility 语义 |
| `load_on_low_memory` | sound object 字段 | 低内存加载行为 | sample/tutorial-backed | O7-SOUND-ADD, C8 | 更适合作为补充字段说明 |

## 5. 写作边界

1. `sounds.json` 与 `sound_definitions.json` 不是一回事，文档必须分开写；这一点既有官方文档，也有仓内 parser 分离证据（I4）。
2. `sounds.json` 更像“事件/类别 wiring”，`sound_definitions.json` 更像“短名 -> 实际声音资源定义”。
3. `sounds.json` 顶层没有统一包装对象，通常直接挂 `entity_sounds`、`block_sounds`、`interactive_block_sounds`、`individual_event_sounds` 等分区。
4. 嵌套目录如 `sounds/component/...`、`sounds/tile/...`、以及 `music_definitions.json` 更适合作为样例组织细节，不应写成每个声音包的核心入口。

## 6. 主要来源编号

- O7-SOUND-INTRO
- O7-SOUND-ADD
- C7
- C8
- I4
- R4
- R7
