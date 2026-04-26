# Bedrock Add-On 资源文件层：materials

## 1. 适用范围

本页讨论 Bedrock 里的 material 相关语义，但要先明确：

> 这一族不像 `render_controllers`、`particles` 那样有清晰稳定的“资源包内标准入口 + 独立 JSON 结构参考页”。

## 2. 位置矩阵

| 项 | 位置层级 | 类型 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `.material` 文件 | 引擎/高级资源层 | 文件 | advanced reference | O7-MATERIALS | 官方有技术说明，但不是清晰的标准资源包主线入口说明 |
| `entity.material` | 引擎内部文件 | 内部文件 | internal reference | O7-MATERIALS | 官方明确指出它是内部文件，不对创作者直接开放 |
| `client_entity.description.materials` | RP client entity 语义 | 映射区段 | example/reference hybrid | O7-CEINTRO, C5 | 真实创作者更常接触的是“引用材质名”，而不是直接 author `.material` |
| `particle_effect.description.basic_render_parameters.material` | 粒子语义 | 字段 | stable | O7-PARTICLE-DOC | 粒子最常见的材质引用位置 |

## 3. 主要结构矩阵

| 项 | 层级 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|---|
| `materials` | `.material` 根对象 | 容纳材质定义 | advanced reference | O7-MATERIALS | 更偏技术概览，不是日常 addon 基础教程 |
| `version` | `.material` 字段 | 指示 material 文件版本 | advanced reference | O7-MATERIALS | 技术说明中出现 |
| `child:parent` | 材质继承命名模式 | 表示继承关系 | advanced reference | O7-MATERIALS | 应写成高级特性 |
| `+defines` / `-defines` | 材质定义区段 | 控制 shader define | advanced reference | O7-MATERIALS | 高级字段 |
| `+states` / `-states` | 材质定义区段 | 控制渲染状态 | advanced reference | O7-MATERIALS | 高级字段 |
| `blendSrc` / `blendDst` / `alphaSrc` / `alphaDst` | 材质字段 | 混合模式控制 | advanced reference | O7-MATERIALS | 高级字段 |

## 4. 样例与实践矩阵

| 项 | 作用 | 规范性 | 主要参考 | 备注 |
|---|---|---|---|---|
| `pig_v3` | Mojang client entity 中的材质引用 | sample-backed | R7 | 说明样例更常展示“引用材质名” |
| `entity_alphatest` | Microsoft sample 中的材质引用 | sample-backed | R3 | 说明真实样例经常只引用内建材质 |
| `particles_alpha` | 粒子样例中的材质引用 | stable sample | R7, R4 | 粒子里经常直接引用材质名 |

## 5. 写作边界

1. `materials` 不能写成和 `render_controllers`、`particles` 同级的“标准基础资源包主线入口”。
2. 更准确的说法是：
   - 官方有 material 文件技术说明；
   - 但真实样例更常展示“如何引用材质名”，而不是直接 author `.material` 文件。
3. 自定义 material 应写成 **高级/可能不稳定/不推荐作为入门默认实践**。

## 6. 主要来源编号

- O7-MATERIALS
- O7-CEINTRO
- O7-PARTICLE-DOC
- C5
- R3
- R4
- R7
