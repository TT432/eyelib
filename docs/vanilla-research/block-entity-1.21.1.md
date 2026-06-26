# 1.21.1 (NeoForge) 方块实体渲染系统分析

> 基于 `.local_ref/mc/1.21.1/sources/` 提取源码。所有路径相对于该目录。

## 目录

1. [与 1.20.1 的总体差异](#1-与-1201-的总体差异)
2. [BlockEntityRenderer 接口](#2-blockentityrenderer-接口)
3. [BlockEntityRenderDispatcher](#3-blockentityrenderdispatcher)
4. [渲染器注册变更](#4-渲染器注册变更)
5. [LevelRenderer 调度变更](#5-levelrenderer-调度变更)
6. [光照与工具类](#6-光照与工具类)

---

## 1. 与 1.20.1 的总体差异

1.21.1 的 BE 渲染系统与 1.20.1 **结构几乎完全一致**。核心差异：

| 方面 | 1.20.1 (Forge) | 1.21.1 (NeoForge) |
|---|---|---|
| 平台注解 | `net.minecraftforge.api.distmarker.OnlyIn` | `net.neoforged.api.distmarker.OnlyIn` |
| 渲染器接口继承 | 纯 `BlockEntityRenderer<T>` | `BlockEntityRenderer<T> extends IBlockEntityRendererExtension<T>` |
| Frustum culling hook | 直接调用 `frustum.isVisible()` | 通过 `ClientHooks.isBlockEntityRendererVisible()` |
| `BlockEntityRenderers` 泛型 | `Map<BE, Provider<?>>` | `Map<BE, Provider<?, ?>>` |
| 新增 BE 类型 | 无 | TRIAL_SPAWNER, VAULT |
| SignRenderer | `SignRenderer` 独立类 | `SignRenderer` 独立类（结构不变） |

**核心渲染流程不变**：仍然是即时模式渲染（无 extract/submit 分离）。

## 2. BlockEntityRenderer 接口

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderer.java` (25 行)

```java
public interface BlockEntityRenderer<T extends BlockEntity>
        extends net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension<T> {
    void render(T blockEntity, float partialTick, PoseStack poseStack,
                MultiBufferSource bufferSource, int packedLight, int packedOverlay);

    default boolean shouldRenderOffScreen(T blockEntity) { return false; }
    default int getViewDistance() { return 64; }

    default boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        return Vec3.atCenterOf(blockEntity.getBlockPos())
                .closerThan(cameraPos, (double)this.getViewDistance());
    }
}
```

与 1.20.1 的唯一差异：接口声明增加了 `extends IBlockEntityRendererExtension<T>`，允许 NeoForge 通过接口注入扩展方法。

## 3. BlockEntityRenderDispatcher

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher.java` (132 行)

代码结构与 1.20.1 **完全相同**：

- `render()` 方法流程：`getRenderer → hasLevel && isValid → shouldRender → setupAndRender`
- `setupAndRender()` 调用 `LevelRenderer.getLightColor(level, blockPos)` 获取光照
- `renderItem()` 用 0.0F partialTick 和调用方提供的光照/overlay
- `onResourceManagerReload()` 重建 renderer map

### 方法签名差异

1.21.1 的 `renderItem()` 中 lambda 写法略有简化：
```java
// 1.21.1: lambda 内直接展开 setupAndRender
tryRender(blockEntity, () -> setupAndRender(blockentityrenderer, blockEntity, partialTick, poseStack, bufferSource));
```

```java
// 1.20.1: 用 {} 包裹
tryRender(blockEntity, () -> {
    setupAndRender(blockentityrenderer, blockEntity, partialTick, poseStack, bufferSource);
});
```

语义完全相同。

## 4. 渲染器注册变更

**文件**: `net/minecraft/client/renderer/blockentity/BlockEntityRenderers.java` (63 行)

### 4.1 泛型签名变化

```java
// 1.20.1
private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?>> PROVIDERS = ...;

// 1.21.1
private static final Map<BlockEntityType<?>, BlockEntityRendererProvider<?, ?>> PROVIDERS = ...;
```

`BlockEntityRendererProvider` 在 26.1.2 会有两个泛型参数（`<T, S>`），1.21.1 使用 `<?, ?>` 向前兼容，实际 Provider 仍有 `<T>` 单参数。

### 4.2 新增渲染器

```java
register(BlockEntityType.TRIAL_SPAWNER, TrialSpawnerRenderer::new);
register(BlockEntityType.VAULT, VaultRenderer::new);
```

- `TrialSpawnerRenderer`：渲染试炼刷怪笼的旋转笼子和生物预览。
- `VaultRenderer`：渲染宝库方块。

### 4.3 `createEntityRenderers` 内部

类型转换增加：
```java
builder.put((BlockEntityType<?>)p_339298_, p_339299_.create(context));
```

1.20.1 的 `p_258150_` 参数名变更为 NeoForge 的 `p_339298_` 样式（mapping 差异）。

## 5. LevelRenderer 调度变更

**文件**: `net/minecraft/client/renderer/LevelRenderer.java`

### 5.1 整体渲染流程

与 1.20.1 相同，`renderLevel()` 中的阶段顺序：

```
light_update_queue → light_updates → culling → clear → sky → fog
→ terrain_setup → compile_sections → terrain → entities → blockentities
→ endBatch → destroyProgress → particles → weather
```

1.21.1 使用 `compileSections`（vs 1.20.1 的 `compileChunks`）和 `renderSectionLayer`（vs `renderChunkLayer`）——术语从 Chunk 改为 Section。

### 5.2 BE 调度代码

```java
profilerfiller.popPush("blockentities");

// 从 visibleSections 获取 renderable block entities
for (SectionRenderDispatcher.RenderSection section : this.visibleSections) {
    List<BlockEntity> list = section.getCompiled().getRenderableBlockEntities();
    if (!list.isEmpty()) {
        for (BlockEntity blockentity1 : list) {
            if (!ClientHooks.isBlockEntityRendererVisible(
                    blockEntityRenderDispatcher, blockentity1, frustum)) continue;
            BlockPos pos = blockentity1.getBlockPos();
            // ... poseStack 平移、破坏进度、render
            this.blockEntityRenderDispatcher.render(blockentity1, f, posestack, buffer);
        }
    }
}

// global block entities
synchronized (this.globalBlockEntities) {
    for (BlockEntity blockentity : this.globalBlockEntities) {
        if (!ClientHooks.isBlockEntityRendererVisible(
                blockEntityRenderDispatcher, blockentity, frustum)) continue;
        // ... render
    }
}
```

关键变化：
1. `Frustum.isVisible()` 调用替换为 `ClientHooks.isBlockEntityRendererVisible()` — NeoForge 钩子允许模组控制可见性。
2. `RenderChunkInfo` 改名为 `RenderSection`。
3. `chunk.getCompiledChunk()` 改为 `section.getCompiled()`。
4. 多了一个 `iterateVisibleBlockEntities(Consumer<BlockEntity>)` 工具方法。

### 5.3 新增遍历方法

```java
public void iterateVisibleBlockEntities(Consumer<BlockEntity> blockEntityConsumer) {
    for (var chunkInfo : this.visibleSections)
        chunkInfo.getCompiled().getRenderableBlockEntities().forEach(blockEntityConsumer);
    this.globalBlockEntities.forEach(blockEntityConsumer);
}
```

供 NeoForge 的 `ClientHooks` 使用。

## 6. 光照与工具类

### 6.1 光照 API 不变

- `LevelRenderer.getLightColor(level, blockPos)` 返回 packed int。
- `BlockEntityRenderDispatcher.setupAndRender()` 中无变化。
- `OverlayTexture.NO_OVERLAY` 不变。

### 6.2 BrightnessCombiner

**文件**: 与 1.20.1 完全相同（36 行），无变化。

### 6.3 LightTexture

与 1.20.1 相同，pack 格式不变。

---

## 总结：1.20.1 → 1.21.1 差异矩阵

| 组件 | 变化 |
|---|---|
| BlockEntityRenderer | 增加 `extends IBlockEntityRendererExtension<T>` |
| BlockEntityRenderDispatcher | 无实质变化 |
| BlockEntityRendererProvider.Context | 无变化 |
| BlockEntityRenderers | 泛型 Map 增加 wildcard；新增 TrialSpawner + Vault |
| LevelRenderer 调度 | `ClientHooks.isBlockEntityRendererVisible()` hook；术语 Chunk→Section |
| BrightnessCombiner | 无变化 |
| 光照计算 | 无变化 |
| SignRenderer / ChestRenderer 等 | 无变化（内容相同） |
| TheEndPortalRenderer | 无变化 |

**核心结论**：1.21.1 的 BE 渲染系统是 1.20.1 的轻量端口，保持了相同的即时渲染架构。真正的大重构发生在 **26.1.2**。
