# Bedrock Add-On 资源文件层：render_controllers

## 1. 适用范围

本页只讨论 Resource Pack 中的 render controller 文件，不讨论：

- client entity 里如何引用 render controller
- 样例中放在纹理侧的附属元数据路径

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `render_controllers/` | RP 内容层 | 目录族 | stable | O3, C4 | 官方 starter 与社区解释都支持这个目录作为主入口 |
| `*.render_controllers.json` | RP 内容层 | 文件 | stable sample | R7 | 样例常用命名，说明真实文件通常这样组织 |
| `textures/render_controllers/*.json` | RP 内容层 | 附属样例路径 | sample-only | R4, C4 | 样例存在/项目可管理，但未找到官方稳定标准把它定义为 render controller 主入口 |

## 3. 顶层结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `format_version` | 顶层字段 | 指示文件使用的 render controller 语义版本 | stable | O7-RCREF | 常见为 `1.8.0` |
| `render_controllers` | 顶层对象 | 容纳 controller ID -> controller body 的映射 | stable | O7-RCREF | 正式顶层对象 |
| `controller.render.*` | 对象键 | 单个 render controller ID | stable sample | R7 | 样例常用命名风格，不是语法关键字 |

## 4. 主要区段矩阵

| 字段/区段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `geometry` | 选择要使用的几何体 | stable | O7-RCREF | 核心输出之一 |
| `materials` | 选择材质/材质映射 | stable | O7-RCREF | 核心输出之一 |
| `textures` | 选择纹理 | stable | O7-RCREF | 核心输出之一 |
| `part_visibility` | 控制骨骼/部件可见性 | stable | O7-RCREF | 标准字段，但不是所有样例都会出现 |
| `arrays` | 定义 geometry/material/texture 的命名数组 | tutorial/sample-backed | O7-RCTUT, R7 | 常见且实用，但更偏教程/样例写法 |
| `color` | 整体颜色调制 | tutorial/sample-backed | O7-RCREF, R7 | 可选增强字段 |
| `is_hurt_color` | 受伤颜色调制 | sample-backed | R7, R3 | 样例常见，不应写成基础必备字段 |
| `filter_lighting` | 光照过滤相关行为 | sample-backed | R3 | 样例出现，应写成可选增强字段 |

## 5. 写作边界

1. 可以把 `render_controllers/` 写成标准主入口。
2. 不应把 `textures/render_controllers/*.json` 写成同等级标准入口。
3. `arrays`、`filter_lighting`、`is_hurt_color` 等内容应写成：
   - 样例支持
   - 常见增强用法
   - 非基础最小形状

## 6. 主要来源编号

- O3
- C4
- O7-RCREF
- O7-RCTUT
- R3
- R4
- R7
