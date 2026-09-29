package io.github.tt432.eyelib.snowstorm.texture;

import org.jspecify.annotations.Nullable;

/**
 * {@code TextureClass.source} 串 ↔ 栅格的编解码接缝（规格 §2-D5：PNG 解码留给 MC 侧，
 * domain 只收像素数组+宽高）。
 *
 * <p>JS 中 {@code source} 是 {@code canvas.toDataURL()} 产生的 PNG dataURL，浏览器负责
 * 编解码；Java 侧由 MC 桥安装实现（PNG ↔ dataURL 或任意不透明串）。source 串对 domain
 * 完全 opaque：只参与 history 快照存储与 {@code wintersky TextureImage.setSrc} 回推。
 */
public interface TextureSourceCodec {

    /** {@code canvas.toDataURL()} 等价：栅格 → source 串。 */
    String encode(int[] abgr, int width, int height);

    /**
     * {@code new Image(); img.src = source} 解码 + {@code drawImage} 等价：source 串 → 栅格。
     * 解码失败返回 {@code null}（JS {@code img.onerror} → Promise reject 未捕获，canvas 保持不变）。
     */
    @Nullable DecodedImage decode(String source);

    /** 解码结果：ABGR 像素数组 + 宽高。 */
    final class DecodedImage {
        public final int width;
        public final int height;
        public final int[] abgr;

        public DecodedImage(int width, int height, int[] abgr) {
            this.width = width;
            this.height = height;
            this.abgr = abgr;
        }
    }
}
