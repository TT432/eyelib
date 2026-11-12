## 装备渲染四个开关 + 代理/真实 attachable 同构化（21.1.41）

**本版为 21.1.40 的补齐**（21.1.40 把原版装备渲染收敛为「代理 attachable」，本版把基岩版
`minecraft:client_entity.description` 的四个装备渲染字段全部实现，并让代理 attachable 与真实 attachable
共用同一套判定）：

| 字段 | 语义 | 落点 |
|---|---|---|
| `enable_attachables` | 盔甲与武器的总开关（缺省 false） | `AttachablePolicy.allowsAttachables` |
| `hide_armor` | 覆盖总开关的盔甲部分：只压盔甲，武器照常 | `AttachablePolicy.allowsArmor` |
| `held_item_ignores_lighting` | 手持物全亮 | `AttachablePolicy.heldItemLight` |
| `held_item_scale` | Molang 缩放手持物（<=0 回落 1.0） | `AttachablePolicy.heldItemScale` |

抽象方式：① 准入只有 `AttachableResolver.resolve(holder, stack, slot)` 一处，内部按 `isArmorStack` 分流到
`allowsArmor` / `allowsAttachables`，代理侧调用同一个判定；② 挂载点解析只有 `ProxyAttachable.resolveChain`
一处（真实 attachable 的 `binding` / 同名骨骼是单元素链）；③ 槽位归属仍由 `ProxyAttachments` 裁决，
真实 attachable 命中的槽整条跳过。

**验证**：三节点 `compileJava` / `:test` / `:nullawayMain` 全绿；26.1.2 实机逐字段改写 description 验证
`hide_armor`（铁盔/胸甲不显示、铁剑仍显示）、`enable_attachables:false`（甲与剑皆无，同帧对照 skeleton 正常）、
`held_item_ignores_lighting`（午夜剑亮白）、`held_item_scale:"3.0"`（剑明显变长）。

**前置：配合 dokimobs 0.3.16**（dokimobs 的 27 个萌化实体开启 `enable_attachables` 后由本版字段控制显隐与缩放）。
