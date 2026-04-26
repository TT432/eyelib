# Bedrock Add-On 参考

## 范围

这一组文档只回答两类基础问题：

1. Bedrock Add-On **都有哪些属性**。
2. Bedrock Add-On **都是什么结构**。

本阶段先聚焦“容器、manifest、包结构、资源族、真实样例”，并开始向资源文件层继续细化。

## 文档列表

- `都有哪些属性.md`：以逐字段参考矩阵的形式整理 addon / pack / manifest 级别属性，包含作用、适用范围、稳定性与来源编号。
- `都是什么结构.md`：以结构参考矩阵的形式整理 `.mcaddon` / `.mcpack` / 文件夹、BP/RP、目录树与文件族。
- `资源文件层-README.md`：资源文件层参考索引。
- `资源文件层-render_controllers.md`：render controller 资源文件层参考。
- `资源文件层-client_entity.md`：client entity 资源文件层参考。
- `资源文件层-particles.md`：particle 资源文件层参考。
- `资源文件层-sounds.md`：sounds 资源文件层参考。
- `资源文件层-materials.md`：materials 资源文件层参考。
- `资源文件层-animations.md`：animations 资源文件层参考。
- `资源文件层-animation_controllers.md`：animation controllers 资源文件层参考。
- `资源文件层-geometry-models.md`：geometry / models 资源文件层参考。
- `资源文件层-attachables.md`：attachables 资源文件层参考。
- `资源文件层-localization.md`：`texts/*.lang` 本地化资源参考。
- `资源文件层-texture_indices.md`：texture indices 资源文件层参考。
- `资源文件层-ui-fogs.md`：UI / fogs 资源文件层参考。
- `行为文件层-README.md`：行为文件层参考索引。
- `行为文件层-behavior_entities.md`：behavior entity 文件层参考。
- `行为文件层-表类资源.md`：blocks / items / recipes / loot_tables / trading / spawn_rules / structures 参考。
- `递归字段索引-manifest.md`：容器 / pack / manifest 递归字段索引。
- `递归字段索引-rp-entity-render.md`：RP 实体、渲染、动画、几何体递归字段索引。
- `递归字段索引-rp-media.md`：RP 粒子、声音、材质、纹理索引、fogs、UI 递归字段索引。
- `递归字段索引-bp-entity-table.md`：BP 实体、spawn_rules、blocks/items/recipes/loot_tables/trading 等递归字段索引。
- `递归字段索引-importer-support.md`：Eyelib importer 当前 typed/partial/raw/unmanaged 支持面。
- `implementation-effect-tracker.ai.yaml`：面向 AI 维护的 BedrockAddon“实际生效/缺口/桥接机会”追踪文件。
- `来源清单.md`：列出官方文档、社区文档和真实样例仓库来源。

## 阅读顺序

1. 先看 `都是什么结构.md`，按结构矩阵建立容器 / pack / 目录族 / 文件族的层级感。
2. 再看 `都有哪些属性.md`，按逐字段矩阵理解 manifest 与 pack 级属性。
3. 再看 `资源文件层-README.md` 和各资源族专题页，理解文件层结构。
4. 再看 `行为文件层-README.md` 和对应专题页，理解 Behavior Pack 文件层结构。
5. 需要继续往“字段 / 子字段 / registry frontier”下钻时，看 `递归字段索引-*.md`。
6. 需要核对“这是不是 stable / preview / sample-only”时，回到 `来源清单.md`。

## 推荐用法

- 想知道“这个东西是不是 manifest 字段” → 先看 `都有哪些属性.md`
- 想知道“这个东西是不是目录 / 文件族 / 导入容器” → 先看 `都是什么结构.md`
- 想知道“这个资源文件应该放哪儿、顶层对象是什么、哪些字段只是样例做法” → 看 `资源文件层-*.md`
- 想知道“这个行为包 JSON 应该放哪儿、顶层对象是什么、有哪些核心区段” → 看 `行为文件层-*.md`
- 想知道“这个节点还能不能继续拆，已经拆到哪一层” → 看 `递归字段索引-*.md`
- 想知道“这是 Bedrock 官方字段，还是 Eyelib 目前只是 raw/partial 支持” → 看 `递归字段索引-importer-support.md`
- 想知道“这个说法到底是官方稳定、preview，还是样例/社区实践” → 对照 `来源清单.md`
