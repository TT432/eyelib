# Bedrock Add-On 行为文件层：behavior_entities

## 1. 适用范围

本页讨论 Behavior Pack 中的实体行为文件，也就是以 `minecraft:entity` 为顶层对象的实体定义文件。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `entities/` | BP 内容层 | 目录族 | stable | O4, O8-COMPPACK, R8 | 行为实体主目录 |
| `*.json` | BP 内容层 | 文件 | stable sample | O4, R8, R4 | 样例常见命名形式 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示行为实体文档版本 | stable | O4, S3, R4 | 常见如 `1.16.0` |
| `minecraft:entity` | 顶层对象 | 容纳行为实体定义 | stable | O4, S3, R4 | 行为实体主对象 |
| `description` | 主体对象下区段 | 定义实体身份与生成相关属性 | stable | O4, S3, R4 | 最核心区段 |
| `component_groups` | 主体对象下区段 | 定义可切换的组件组 | stable sample / schema-backed | S3, R4 | 常见高级区段 |
| `components` | 主体对象下区段 | 定义实体行为与能力 | stable | O4, S3, R4 | 最核心区段 |
| `events` | 主体对象下区段 | 定义事件驱动变化 | stable sample / schema-backed | S3, R4 | 常见高级区段 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `description.identifier` | 实体 ID | stable | O4, S3, R4 | 核心字段 |
| `description.is_spawnable` | 是否自然/规则生成 | stable sample / schema-backed | S3, R4 | 常见区段 |
| `description.is_summonable` | 是否可召唤 | stable sample / schema-backed | S3, R4 | 常见区段 |
| `description.is_experimental` | 实验性标记 | sample-backed / schema-backed | S3, R4 | 应写成可选区段 |
| `description.runtime_identifier` | 运行时绑定的基础实体 | sample-backed | R4 | 官方样例常见，但不应写成所有实体都必备 |
| `description.properties` | 自定义属性定义 | stable sample / schema-backed | S3, R4 | 高级常见区段 |
| `components` | 组件集合 | stable | O4, S3, R4 | 核心行为层 |
| `component_groups` | 组件组集合 | stable sample / schema-backed | S3, R4 | 用于状态切换 |
| `events` | 事件集合 | stable sample / schema-backed | S3, R4 | 用于 add/remove component groups、emit particle、play sound 等 |

## 5. 写作边界

1. 这里先写“行为实体文件层”，不在本页继续拆每个 `minecraft:*` 组件。
2. 组件层是下一层递归节点，只有在需要时再继续细分；如果要继续下钻，优先从 `components`、`events`、`spawn_rules` 三个相邻体系展开。
3. 现在可以把字段来源分成三层：
   - S3：官方机器可读 schema，适合确认对象名、层次和可选字段；
   - O4 与相关实体 reference 页：官方 prose/reference，适合确认概念和入口路径；
   - R4 / 官方样例：适合证明 `runtime_identifier`、`emit_particle`、`play_sound`、`randomize` 这类现实写法。
4. 当前最稳妥的写法是：
   - 行为实体文件主结构稳定可描述；
   - 具体组件表属于下一层专题。

## 6. 主要来源编号

- S3
- O4
- O8-COMPPACK
- R4
- R8
