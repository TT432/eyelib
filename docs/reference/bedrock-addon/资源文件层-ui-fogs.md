# Bedrock Add-On 资源文件层：UI / fogs

## 1. 适用范围

本页把 `ui` 和 `fogs` 暂时放在同一页，作为资源包里已经出现、但当前项目关注度低于实体/动画族的两类资源族。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `ui/` | RP 内容层 | 目录族 | stable sample | R7, O8-COMPPACK | 样本库可见的 UI 资源目录 |
| `fogs/` | RP 内容层 | 目录族 | stable sample | R7, O8-COMPPACK | 样本库可见的 fog 资源目录 |
| `ui/*.json` / `ui/*.png` / `ui/*.jpg` | RP 内容层 | 文件 | stable sample | O8-COMPPACK, R7 | UI 相关文件族 |
| `fogs/*.json` | RP 内容层 | 文件 | stable sample | O8-COMPPACK, R7 | fog 文件族 |

## 3. 当前可确认的写法边界

| 项 | 当前可确认内容 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| UI 文件族存在 | 官方样本与 pack contents 页都表明 `ui/` 是真实存在的 RP 文件族 | stable sample | O8-COMPPACK, R7 | 当前更像“结构可确认”，字段级参考还不够 |
| Fog 文件族存在 | 官方样本与 pack contents 页都表明 `fogs/` 是真实存在的 RP 文件族 | stable sample | O8-COMPPACK, R7 | 当前更像“结构可确认”，字段级参考还不够 |

## 4. 写作边界

1. 这两个族当前可以先写成“结构已确认、字段仍待继续展开”的伞页。
2. `fogs` 现在已经有足够字段级证据，可直接参考 `递归字段索引-rp-media.md` 中的 `minecraft:fog_settings` 树；该树的顶层对象和必选字段已经能回溯到官方 schema（S5）。
3. `ui` 也已经能递归到 screen file 入口与常见元素字段层，但仍保留大量屏幕/控件 registry；因此更适合在递归索引页继续展开，而不是在这张伞页上硬写成统一 prose 规范。当前最稳的官方来源是 RP UI schema（S6）和官方样例目录（R7）。
4. 如果后续需要专题化，优先把这一页拆成 `资源文件层-ui.md` 与 `资源文件层-fogs.md` 两页。

## 5. 主要来源编号

- O8-COMPPACK
- S5
- S6
- R7
