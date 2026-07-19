package io.github.tt432.eyelib.bridge.client.render.texture.adapter;

import com.mojang.blaze3d.platform.NativeImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeImageIOTest {

    @Test
    void clampAlphaToBinaryKeepsFullyTransparentPixelsTransparent() {
        // A&S 用 1×1 透明占位图（alpha=0 的白色像素）实现不可见渲染层，
        // clamp 不得将其抬升为不透明（BE 语义：alpha=0 即不可见）。
        try (NativeImage image = new NativeImage(NativeImage.Format.RGBA, 2, 2, false)) {
            image.setPixelRGBA(0, 0, 0x00FFFFFF); // alpha=0 白色（透明占位）
            image.setPixelRGBA(1, 0, 0x00000000); // alpha=0 黑色
            image.setPixelRGBA(0, 1, 0x03132644); // alpha=3 低透明抗锯齿 → 255
            image.setPixelRGBA(1, 1, 0xFF132644); // alpha=255 不变

            NativeImageIO.clampAlphaToBinary(image);

            assertEquals(0x00FFFFFF, image.getPixelRGBA(0, 0), "alpha=0 必须保持透明");
            assertEquals(0x00000000, image.getPixelRGBA(1, 0), "alpha=0 黑色保持透明");
            assertEquals(0xFF132644, image.getPixelRGBA(0, 1), "低 alpha 应抬升为 255");
            assertEquals(0xFF132644, image.getPixelRGBA(1, 1), "不透明像素不变");
        }
    }
}
