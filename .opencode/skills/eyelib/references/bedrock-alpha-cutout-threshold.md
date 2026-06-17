# Bedrock 低 alpha 纹理 + MC cutout 阈值不匹配

## 症状
- 某些骨骼/面的 mesh 完全不渲染（"肉部分缺失"、"只有羊毛没有身体"）
- 剪毛后 body 骨骼显示为"蓝色碎块"（只有极少数像素通过 cutout）
- 程序化检查全部通过：part_visibility 全 true、bake 正确、RenderType 正确

## 根因
Bedrock addon 纹理（如 Actions & Stuff 的 cor.png）在 body 骨骼 UV 区域使用了 **alpha=3 的 faint 像素**做边缘抗锯齿。MC 的 `rendertype_entity_cutout_no_cull.fsh` 中：

```glsl
if (color.a < 0.5) { discard; }
```

threshold = 0.5（128/255）。alpha=3 ≈ 0.012 远低于阈值，全部被 discard。

Bedrock 引擎的 ALPHA_TEST 阈值极低（接近 alpha > 0），所以 alpha=3 在 Bedrock 中完全可见。

## 诊断方法

**Phase A: Alpha cutout 排查**

```java
// 1. 查 body 骨骼 face UV
io.github.tt432.eyelibmodel.Model model = (io.github.tt432.eyelibmodel.Model) 
    io.github.tt432.eyelib.client.manager.ModelManager.INSTANCE.get("geometry.oreville_ans.rmflah");
Object bone = model.allBones().get(GlobalBoneIdHandler.get("46fljga5"));
// 读取 face0 vertex0 的 UV
java.util.List faces = (java.util.List) cube.getClass().getMethod("faces").invoke(cube);
Object v0 = ((java.util.List) faces.get(0).getClass().getMethod("vertexes").invoke(faces.get(0))).get(0);
Vector2fc uv = (Vector2fc) v0.getClass().getMethod("uv").invoke(v0);

// 2. 查运行时纹理该 UV 位置的 alpha
NativeImage img = ...downloadTexture...;
int rgba = img.getPixelRGBA((int)(uv.x() * w), (int)(uv.y() * h));
int alpha = (rgba >> 24) & 0xFF;

// 3. 扫描 face 覆盖的 UV 区域 opaque 比例
// 如果 opaque/total < ~30%，大部分像素被 discard → 几乎不可见
```

## 修复

在 `NativeImageIO.upload()` 和 `fromImportedImageData()` 中 clamp alpha 到 binary（0→0, >0→255）：

```java
public void clampAlphaToBinary(NativeImage image) {
    for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
            int rgba = image.getPixelRGBA(x, y);
            int alpha = (rgba >> 24) & 0xFF;
            if (alpha > 0 && alpha < 255) {
                image.setPixelRGBA(x, y, rgba | 0xFF000000);
            }
        }
    }
}
```

两处调用：`upload()`（覆盖 TextureLayerMerger 合成路径）和 `fromImportedImageData()`（覆盖 addon 纹理直传路径）。

## 不是根因的排查路径

- **box UV 计算错误** → box UV 计算代码 `bedrockBoxUv()` 逻辑正确
- **material_instance 过滤** → eyelib 不按 material_instance 过滤 face
- **TwoSideModelBakeInfo 跳过 cube** → twoSide 数组长度始终等于 cubes.size()
- **深度测试互斥** → 同 component 内骨骼按层级遍历，子骨骼深度测试正常
- **纹理丢失/多层合并** → cor.png 是唯一纹理，无多层合成
