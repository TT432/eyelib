# Bedrock Add-On 资源文件层：texture indices

## 1. 适用范围

本页讨论纹理侧索引文件，而不是单个 `.png` 纹理本身。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `textures/item_texture.json` | RP 内容层 | 文件 | stable tutorial/sample | O8-ITEMS, O8-BLOCKS, R7 | 物品纹理索引 |
| `textures/terrain_texture.json` | RP 内容层 | 文件 | stable tutorial/sample | O8-BLOCKS, R7 | 地形/方块纹理索引 |
| `textures/flipbook_textures.json` | RP 内容层 | 文件 | stable sample | R7 | 动画纹理索引 |
| `textures/texture_list.json` | RP 内容层 | 文件 | stable sample | R7 | 纹理列表文件 |
| `blocks.json` | RP 内容层 | 文件 | stable tutorial/sample | O8-BLOCKS, R7 | 方块相关资源索引/配置文件 |
| `biomes_client.json` | RP 内容层 | 文件 | stable sample / deprecated note | R7 | 文档中有 deprecated 提示时应显式标出 |

## 3. 主要结构矩阵

| 文件 | 顶层对象/关键字段 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `item_texture.json` | `resource_pack_name`、`texture_data` | 建立物品纹理短名 -> 路径映射 | stable tutorial/sample | O8-ITEMS | 常见于自定义物品流程 |
| `terrain_texture.json` | `resource_pack_name`、`texture_name`、`texture_data` | 建立方块/地形纹理映射 | stable tutorial/sample | O8-BLOCKS | 常见于自定义方块流程 |
| `blocks.json` | `format_version`、namespaced block key | 设置方块资源侧行为（如声音） | stable tutorial/sample | O8-BLOCKS | 不是 behavior block 文件 |
| `flipbook_textures.json` | 顶层数组/对象结构 | 描述动画纹理播放 | stable sample | R7 | 更偏样本库存在 |
| `texture_list.json` | 顶层列表/对象结构 | 纹理列表 | stable sample | R7 | 更偏样本库存在 |

## 4. 写作边界

1. 不要把每个 texture index 文件拆成单独一页，先作为一个 family page 管理。
2. `blocks.json` 属于纹理/资源侧索引文件，不是 behavior pack 的 block 行为定义。
3. `biomes_client.json` 这类文件应在文档里带上 deprecated/sample 边界，避免冒充主线入口。

## 5. 主要来源编号

- O8-ITEMS
- O8-BLOCKS
- R7
