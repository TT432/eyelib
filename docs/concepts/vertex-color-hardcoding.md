# 顶点颜色硬编码与 Per-Entity Tint 修复

## 症状

所有使用 Bedrock 模型的实体渲染为默认纹理颜色，无法应用顶点着色（如羊的染色、马的标记色等）。具体表现为"羊毛染色无效"——蓝色羊显示为白色/灰色。

## 根因

Bedrock `.geo.json` 格式本身**不含顶点颜色数据**。从 Model → BakedBone → VertexConsumer 全链路硬编码顶点颜色为 `(1.0, 1.0, 1.0, 1.0)`：

```java
// HighSpeedRenderModelVisitor.java:64
consumer.vertex(..., 1, 1, 1, 1, ...);  // RGBA 硬编码白色
```

```java
// BakedModel.java:57
vertices.vertex(xList[i], yList[i], zList[i], 1, 1, 1, 1, ...);
```

## 修复方案：Per-Entity Tint（2026-06-05 已实现）

不走全链路顶点颜色（Bedrock 格式不支持），而是在渲染时根据实体类型注入 tint color。

### 修改的文件

1. **`RenderParams.java`** — 新增 `@Nullable float[] tintColor` 字段 + Builder setter
2. **`SimpleRenderAction.java`** — 新增 `entityTintColor()` 方法，从 entity 提取颜色（如 `Sheep.getColor().getTextureDiffuseColors()`）
3. **`HighSpeedRenderModelVisitor.java`** — `visitVertex()` 接受 `tintColor` 参数，非 null 时使用，否则默认白色

### 数据流

```
Sheep.getColor() → DyeColor.getTextureDiffuseColors() → float[4]
    ↓
SimpleRenderAction.entityTintColor()
    ↓
RenderParams.tintColor()
    ↓
HighSpeedRenderModelVisitor.visitVertex(bakedBone, consumer, overlay, light, tintColor)
    ↓
consumer.vertex(..., r, g, b, a, ...)
```

### 当前支持的实体

| 实体 | 颜色来源 |
|---|---|
| Sheep | `sheep.getColor().getTextureDiffuseColors()` |

扩展其他实体时在 `SimpleRenderAction.entityTintColor()` 中添加 `instanceof` 分支。

### 局限性

- **全局 tint**：整模 tint，无法按骨骼/部位区分（如羊的脸不应染色但也被 tint）
- 不支持 `USE_COLOR_MASK` shader——该 define 需要自定义 shader 支持，当前 eyelib 使用 MC 标准 RenderType shader

### /eval 验证

```bash
# 验证染色修复后效果
echo 'net.minecraft.world.entity.Entity target = mc.level.getEntity(SHEEP_ID);
net.minecraft.world.entity.animal.Sheep sheep = (net.minecraft.world.entity.animal.Sheep) target;
float[] c = sheep.getColor().getTextureDiffuseColors();
return "color=" + sheep.getColor().getName() + " rgb=" + String.format("%.2f,%.2f,%.2f", c[0], c[1], c[2]);' | curl ...
```
