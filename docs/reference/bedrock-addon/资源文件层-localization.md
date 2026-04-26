# Bedrock Add-On 资源文件层：localization

## 1. 适用范围

本页当前只讨论 **Resource Pack** 中的本地化文本文件，也就是 `texts/*.lang` 这一族资源。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `texts/` | RP 内容层 | 目录族 | stable | O3, O8-ENTITYTYPES | 官方教程明确出现的资源目录 |
| `texts/en_US.lang` | RP 内容层 | 文件 | stable sample | O8-ENTITYTYPES, R7, I7 | 官方教程和官方样例/fixture 都能看到 |
| `texts/*.lang` | RP 内容层 | 文件族 | importer-backed / sample-backed | I1, I2, I6, I7 | 仓内 importer 已按 `texts/` → `LOCALIZATION` 分类并解析 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `key=value` 行 | 单行条目 | 定义文本键到本地化字符串的映射 | importer-backed / sample-backed | I6, I7 | 仓内解析器按 `=` 切分键和值 |
| `# ...` 注释行 | 单行条目 | 注释/说明 | importer-backed | I6 | 解析器会跳过注释行 |
| 空行 | 单行条目 | 可读性分隔 | importer-backed | I6 | 解析器会跳过空行 |

## 4. 写作边界

1. 这一页当前主要回答“路径和文件形态”，不是要把所有文本 key 名都扩成官方语义表。
2. 现有最硬的官方证据是 `texts/en_US.lang` 这类路径会出现在官方教程和官方样例工作流里；更细的语法细节目前主要靠仓内 importer 解析逻辑与 fixture 验证支撑。
3. Behavior Pack 侧也会出现 `texts/` 目录，但这一页暂不展开 BP 本地化约定；BP 路径层事实仍以 `都是什么结构.md` 为准。
4. 因此本页应写成“资源族存在 + 文件形态已知 + 仓内已可解析”，而不是“每个 key 的语义都有官方字段专页”。

## 5. 主要来源编号

- O3
- O8-ENTITYTYPES
- R7
- I1
- I2
- I6
- I7
