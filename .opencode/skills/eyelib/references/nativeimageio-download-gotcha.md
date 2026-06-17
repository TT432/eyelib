# NativeImageIO.download() try-with-resources 陷阱

## 症状

`clampedTexture` 中 `NativeImageIO.download(original, img -> img)` 返回的 `NativeImage` 在后续 `clampAlphaToBinary` 调用中崩溃：
```
java.lang.IllegalStateException: Image is not allocated.
    at com.mojang.blaze3d.platform.NativeImage.getPixelRGBA(NativeImage.java:190)
    at io.github.tt432.eyelib.client.render.texture.NativeImageIO.clampAlphaToBinary(NativeImageIO.java:90)
```

## 根因

`NativeImageIO.download()` 内部使用 try-with-resources：
```java
try (NativeImage nativeImage = new NativeImage(width, height, false)) {
    nativeImage.downloadTexture(0, false);
    return imageFunction.apply(nativeImage);
} // ← nativeImage 在此关闭
```

`img -> img` 返回同一个引用，但 NativeImage 在 return 后立即被 try-with-resources 关闭——后续任何对它的操作都会触发 "Image is not allocated"。

## 正确用法

必须用 `NativeImageIO::copyImage` 在 lambda 内创建深拷贝：
```java
NativeImage downloaded = NativeImageIO.download(original, NativeImageIO::copyImage);
```

`copyImage()` 逐像素复制，返回独立副本，不受 try-with-resources 释放影响。
