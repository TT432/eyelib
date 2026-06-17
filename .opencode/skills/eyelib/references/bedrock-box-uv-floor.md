# Bedrock Box UV: cube size 被 floor 处理

来源：`/mnt/e/_____基岩版文档/bedrock-wiki/docs/visuals/bedrock-modeling.md:16`

> Sometimes the texture on some (smaller) faces is glitched or invisible. This is because **the size of cubes is floored for the UV map calculation**. This means that any size smaller than 1 will result in a 0 pixel wide UV map, which will look glitchy.

## 影响

`ImportedModelData.bedrockBoxUv()` 中 `cube.size()` 直接使用原始浮点值：

```java
// 错误：use raw float
float dx = cube.size().x;  // 1.8 → 1.8px UV

// 正确：floor for Bedrock compatibility  
float dx = (float) Math.floor(cube.size().x);  // 1.8 → 1px UV
```

## 症状

- 眼睛 cube `size=[3, 1.8, 0]`：V 方向 1.8px → 渲染时覆盖 2 个像素行，眼睛"放大"
- 修复后 `floor(1.8)=1`：V 方向 1px，眼睛正常大小

## 修复

`ImportedModelData.java:630-632`，在 `bedrockBoxUv()` 开头 floor size 值。

## 注意

- 只影响 float size（如 1.8、0.5 等）。整数 size 不受影响
- `inflate` 不影响 UV 计算，只影响顶点位置
