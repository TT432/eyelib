# 多 ModelComponent 渲染 z-fighting 诊断

## 根因

MC `MultiBufferSource.BufferSource.getBuffer(RenderType)` 内部机制：

1. **`fixedBuffers`**：28 个预注册的标准 `RenderType → BufferBuilder` 映射（如 `RenderType.entityCutout()`、`RenderType.entityTranslucent()` 等，均不带 texture 参数）
2. **fallback `builder`**：单个 `BufferBuilder`，所有未命中 fixedBuffers 的 RenderType 共享

当 `RenderType.entityCutoutNoCull(texture)` 创建时，带具体 texture 参数 → 产生新的 RenderType 实例，与 fixedBuffers 中的标准实例 hash 不同 → `getBuffer()` 内部 `fixedBuffers.containsKey()` 返回 false → 返回 fallback builder。

**两个 ModelComponent（不同 material → 不同 RenderType 实例）都回退到同一个 fallback BufferBuilder → 顶点数据合并为一次 draw call → z-fighting。**

## 诊断验证

```java
// /eval 验证 fixedBuffers 命中情况
var mc = Minecraft.getInstance();
var bs = mc.renderBuffers().bufferSource();
var rt0 = modelComponent0.getRenderType(texture);  // entityCutoutNoCull(texture)
var rt1 = modelComponent1.getRenderType(texture);   // entityTranslucent(texture)
var buf0 = bs.getBuffer(rt0);
var buf1 = bs.getBuffer(rt1);
// buf0 == buf1 → true（同一 fallback builder）← z-fighting 根因
// buf0 identityHash == buf1 identityHash → 同一对象
```

## 修复

`EntityRenderSystem.renderComponents()` — 每个 ModelComponent 渲染后调用 `endBatch()`：

```java
data.extraRender().render(renderHelper, data);

// 刷新当前 fallback buffer，确保不同 RenderType 的 ModelComponent 使用独立 draw call
if (multiBufferSource instanceof MultiBufferSource.BufferSource bs) {
    bs.endBatch();
}
```

**效果**：endBatch() 提交所有已缓存的顶点（包括 fixedBuffers 和 fallback builder），下一个 component 获取新的 BufferBuilder。虽然 builder 对象可能复用（同一 identity hash），但顶点数据已独立提交。

## 副作用

- `endBatch()` 同时提交 fixedBuffers 中的所有 BufferBuilder（不仅仅是 fallback builder）→ 若其他渲染代码在 fixedBuffers 中有待提交数据，可能被提前提交
- 仅在存在 ≥2 个 ModelComponent 时生效
- 不影响共享同一 RenderType 的 component（那些会自然走到 fixedBuffers 的同一 slot，本就应合并）
