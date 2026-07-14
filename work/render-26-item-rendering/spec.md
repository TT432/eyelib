# 26.1.2 物品渲染路径补全（§7.3）

> **工作单元类型**：执行（规格）
> **输入**：`docs/concepts/cross-version-render-architecture.md` §7.3、§11 实现状态
> **范围**：补全 `renderItemDirect` 在 26.1.2 的实现 + 清理 `flushBuffer` 死代码

## 问题

`EntityRenderSystem.renderItemDirect` 的 `>=26.1` 分支为空（原 `//? if <26.1 {` 块无 else），
导致 eyelib 自管实体（useBuiltInRenderSystem）在 locator bone 上的普通手持物品不渲染。

### 根因（编译器验证，非文档推测）

26.1.2 的 `ItemInHandRenderer.renderItem` **签名变更**：

| 版本 | 签名 |
|------|------|
| `<26.1` | `renderItem(LivingEntity, ItemStack, ItemDisplayContext, boolean left, PoseStack, MultiBufferSource, int)` |
| `>=26.1` | `renderItem(LivingEntity, ItemStack, ItemDisplayContext, PoseStack, SubmitNodeCollector, int)` |

变化：移除 `boolean left`；`MultiBufferSource` → `SubmitNodeCollector`（物品渲染也延迟提交）。

### flushBuffer 已是死代码

`RenderSystemPort.flushBuffer` 零调用者（src + test 均无）。RenderSink 改造后 flush 逻辑移入
`ImmediateRenderSink.flush()`。§7.3 "flushBuffer 待补全" 过时 → 正确处理是删除。

## 调用链（已验证）

```
EntityRenderOrchestrator.renderEntity (extraRender)
  → renderItemInHand(context, action, entity, light)
    → renderHandItemOrAttachable(bufferSource, le, item, ctx, light, pose, left, hand)
      ├─ attachable 路径: AttachableItemRenderSetup.renderAttachable(rd, pose, bufferSource, ...)
      └─ 普通物品: renderHandItem(bufferSource, le, item, ctx, light, pose, left)
        → RenderPorts.get().renderSystemPort().renderItemDirect(le, item, ctx, left, pose, bufferSource, light)
```

## 方案

核心：让 `renderItemDirect` 接收 `RenderSink`（eyelib 抽象）而非 `MultiBufferSource`，
bridge 实现从 sink 取底层 vanilla 渲染目标。application 层零 `//?`（ADR-0016 合规）。

### 改动清单

1. **RenderSink.java** — 加版本特定的底层访问方法：
   - `<26.1`: `MultiBufferSource multiBufferSource()`
   - `>=26.1`: `SubmitNodeCollector submitNodeCollector()`

2. **ImmediateRenderSink.java** — 实现 `multiBufferSource()` → 返回 `bufferSource` 字段

3. **DeferredRenderSink.java** — 实现 `submitNodeCollector()` → 返回 `collector` 字段

4. **EntityRenderPorts.RenderSystemPort** — `renderItemDirect` 签名：`MultiBufferSource` → `RenderSink`

5. **EntityRenderSystem.renderItemDirect** — `//?` 切分方法体：
   - `<26.1`: `renderItem(le, item, ctx, left, pose, sink.multiBufferSource(), light)`
   - `>=26.1`: `renderItem(le, item, ctx, pose, sink.submitNodeCollector(), light)`

6. **EntityRenderOrchestrator** — `renderHandItem` / `renderHandItemOrAttachable` 传 `RenderSink`（来自 `action.sink()`）；attachable 路径仍用 `action.multiBufferSource()`

7. **删除 flushBuffer** — 接口声明 + 实现 + ImmediateRenderSink 注释引用

### 不变量

- application 层（EntityRenderOrchestrator）无 `//?`（版本差异只在 bridge）
- `renderItemDirect` 接口签名三版本统一（接收 RenderSink）
- attachable 渲染路径不受影响（仍用全局 bufferSource）

## 验证

1. `eyelib_debug_build` 1.20.1 + 26.1.2 编译通过
2. 26.1.2 runClient：eyelib 自管实体手持物品可见
3. 1.20.1 回归：手持物渲染不变

## 副作用 / 关联问题

- `ItemInHandRendererMixin` 7参数注入在 26.1.2 不匹配（已记 feedback）—— attachable mixin 路径，独立任务
