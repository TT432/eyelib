# Attachable 实体缓存泄漏修复规格

## 问题模型

`AttachableItemRenderSetup.CACHE` 以 `LivingEntity` 为强键，且 `clearEntity` 没有任何实体卸载调用方。客户端世界中曾经渲染过 attachable 的实体即使离开世界，仍由静态缓存经实体、RenderData、模型组件和 Molang scope 整条对象图保持可达。长时间运行时缓存单调增长，增加堆占用与 GC 压力；既有运行记录最终出现 16 GiB Java heap OOM。

## 功能需求

- 客户端实体离开 level 时必须立即移除其全部 attachable 槽位缓存。
- 即使遗漏离开事件，静态缓存也不得单独阻止实体被 GC。
- 同一实体仍在同一槽位持有同一 `ItemStack` 时必须继续复用现有 `RenderData`。
- `clearEntity` 后再次准备同一物品必须创建新的 `RenderData`。

## 非功能需求

- 不在每帧路径中扫描全缓存。
- 不引入跨线程同步；实体事件与渲染缓存继续由客户端线程使用。
- Forge 1.20.1 与 NeoForge 节点使用各自事件包，公共清理语义一致。

## 前置条件

- `EntityLeaveLevelEvent` 在客户端 level 对象移除实体时触发。
- attachable 缓存仅是派生渲染状态，可在需要时由 `getOrPrepare` 重建。

## 后置条件

- 离开客户端 level 的 `LivingEntity` 不再出现在 attachable 缓存中。
- 未收到事件但已无其他强引用的实体可被弱键缓存回收。

## 不变量

- 槽位失效、第一人称上下文更新和 attachable 解析逻辑不变。
- 服务端 level 事件不操作客户端渲染缓存。

## 异常行为

- 非 `LivingEntity` 离开事件被忽略。
- 清理不存在的实体是幂等操作。

## 副作用

- 离开后若同一实体对象被异常再次渲染，缓存会按需重建；不保留旧动画状态。

## 验证

1. Clientsmoke 验证 `clearEntity` 后同一槽位返回新的 `RenderData`。
2. 构建 1.20.1 与 1.21.1，验证跨版本离开事件接线。
3. 运行 1.20.1 clientsmoke，确认 attachable 端到端测试通过。
