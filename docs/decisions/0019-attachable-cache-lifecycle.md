# ADR-0019: Attachable 派生渲染缓存生命周期

**Status:** Accepted  
**Date:** 2026-07-22  
**Author:** @TT432

## Context

`AttachableItemRenderSetup` 保存的是由实体和当前物品推导出的客户端渲染状态：`RenderData`、模型组件、动画状态和 Molang scope。该状态可以按需重建，不是实体生命周期的权威数据。

如果以 `LivingEntity` 为强键保存静态缓存，实体离开客户端 level 后仍可能通过缓存保持整条对象图可达。仅提供 `clearEntity` 但没有离开事件调用方时，缓存无法随实体生命周期收敛，长时间运行会产生持续的堆压力。

## Decision

1. 缓存使用 `WeakHashMap<LivingEntity, EnumMap<EquipmentSlot, RenderData<ItemStack>>>`。实体没有其他强引用时，缓存不得阻止其回收。
2. 在 Forge/NeoForge 各自的客户端事件适配器中订阅 `EntityLeaveLevelEvent`；仅当 level 是客户端 level 且实体为 `LivingEntity` 时调用 `AttachableItemRenderSetup.clearEntity`。
3. `clearEntity` 删除实体的全部槽位；槽位失效继续由 `invalidate` 处理。清理必须幂等。
4. 缓存命中仍以同一槽位的同一 `ItemStack` 对象为复用条件；清理后再次准备必须创建新的 `RenderData`。
5. 不在每帧扫描缓存，不引入跨线程同步；事件和渲染缓存继续由客户端线程使用。

## Consequences

- 正常离开客户端 level 时，派生 attachable 状态立即释放；即使事件遗漏，弱键也不会单独阻止实体 GC。
- 同一实体和槽位仍保持既有的 `RenderData` 复用语义；清理后旧动画状态不再保留。
- 清理接线属于版本适配层，公共缓存语义不随 Forge/NeoForge 事件包差异改变。
- 离开后异常再次渲染时会按需重建状态，这是预期行为。

## Verification

- 实现：[`AttachableItemRenderSetup`](../../src/main/java/io/github/tt432/eyelib/client/render/AttachableItemRenderSetup.java)、[`EntityLeaveLevelEventAdapter`](../../src/main/java/io/github/tt432/eyelib/bridge/client/render/adapter/EntityLeaveLevelEventAdapter.java)。
- 运行时合同：[`AttachableSmoke`](../../src/main/java/io/github/tt432/eyelib/smoke/AttachableSmoke.java) 覆盖显式清理后重建和离开事件后的重建。
