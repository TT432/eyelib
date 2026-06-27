package io.github.tt432.eyelib.bridge.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelib.importer.model.importer.ImportedImageData;
import io.github.tt432.eyelib.util.color.ColorEncodings;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import org.jspecify.annotations.Nullable;
//? if >=26.1 {
import com.mojang.blaze3d.opengl.GlTexture;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
//?}

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author TT432
 */
@UtilityClass
public class NativeImageIO {
    //? if <26.1 {
    private static final Map<String, ResourceLocation> COLOR_MASK_CACHE = new HashMap<>();
    //?} else {
    private static final Map<String, Identifier> COLOR_MASK_CACHE = new HashMap<>();
    //?}

    //? if <26.1 {
    public void upload(ResourceLocation texture, NativeImage image) {
    //?} else {
    public void upload(Identifier texture, NativeImage image) {
    //?}
        //? if <26.1 {
        DynamicTexture dynamicTexture = new DynamicTexture(image);
        //?} else {
        DynamicTexture dynamicTexture = new DynamicTexture(() -> "eyelib", image);
        //?}
        Minecraft.getInstance().getTextureManager().register(texture, dynamicTexture);
    }

    public void upload(String texture, NativeImage image) {
        //? if <1.20.6 {
        upload(new ResourceLocation(texture), image);
        //?} else {
        upload(Identifier.parse(texture), image);

        //?}
    }

    @Nullable
    //? if <26.1 {
    public <R> R download(ResourceLocation texture, Function<NativeImage, R> imageFunction) {
    //?} else {
    public <R> R download(Identifier texture, Function<NativeImage, R> imageFunction) {
    //?}
        //? if <26.1 {
        Minecraft.getInstance().getTextureManager().getTexture(texture).bind();

        int[] width = new int[1];
        int[] height = new int[1];
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);

        if (width[0] != 0 && height[0] != 0) {
            try (NativeImage nativeImage = new NativeImage(width[0], height[0], false)) {
                nativeImage.downloadTexture(0, false);
                return imageFunction.apply(nativeImage);
            }
        }

        return null;
        //?} else {
        try {
            var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resourceOpt.isPresent()) {
                try (InputStream is = resourceOpt.get().open()) {
                    return imageFunction.apply(load(is));
                }
            }
        } catch (IOException e) {
            // fall through
        }

        try {
            AbstractTexture abstractTexture = Minecraft.getInstance().getTextureManager().getTexture(texture);
            var gpuTexture = abstractTexture.getTexture();
            if (gpuTexture instanceof GlTexture glTexture) {
                NativeImage image = readBackTexture(glTexture.glId(), glTexture.getWidth(0), glTexture.getHeight(0));
                return imageFunction.apply(image);
            }
        } catch (Exception ignored) {
        }

        return null;
        //?}
    }

    //? if >=26.1 {
    static NativeImage readBackTexture(int glId, int width, int height) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = (x + y * width) * 4;
                int r = Byte.toUnsignedInt(buffer.get(offset));
                int g = Byte.toUnsignedInt(buffer.get(offset + 1));
                int b = Byte.toUnsignedInt(buffer.get(offset + 2));
                int a = Byte.toUnsignedInt(buffer.get(offset + 3));
                image.setPixel(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }

        return image;
    }
    //?}

    public NativeImage load(InputStream inputStream) throws IOException {
        var bufferedImage = ImageIO.read(inputStream);
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Invalid buffer");
        }

        var image = new NativeImage(NativeImage.Format.RGBA,
                                    bufferedImage.getWidth(), bufferedImage.getHeight(), false);
        var w = bufferedImage.getWidth();
        var h = bufferedImage.getHeight();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                //? if <26.1 {
                image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(bufferedImage.getRGB(x, y)));
                //?} else {
                image.setPixel(x, y, ColorEncodings.argbToAbgr(bufferedImage.getRGB(x, y)));
                //?}
            }
        }

        return image;
    }

    public NativeImage fromImportedImageData(ImportedImageData imageData) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, imageData.width(), imageData.height(), false);
        for (int x = 0; x < imageData.width(); x++) {
            for (int y = 0; y < imageData.height(); y++) {
                //? if <26.1 {
                image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(imageData.getPixelArgb(x, y)));
                //?} else {
                image.setPixel(x, y, ColorEncodings.argbToAbgr(imageData.getPixelArgb(x, y)));
                //?}
            }
        }
        return image;
    }

    public void loadAndUpload(String textureKey, InputStream inputStream) throws IOException {
        upload(textureKey, load(inputStream));
    }

    public void uploadFromImportedImageData(String textureKey, ImportedImageData imageData) {
        upload(textureKey, fromImportedImageData(imageData));
    }

    @Nullable
    //? if <26.1 {
    public ResourceLocation colorMaskTexture(ResourceLocation texture, float[] color) {
    //?} else {
    public Identifier colorMaskTexture(Identifier texture, float[] color) {
    //?}
        String cacheKey = texture + "/" + colorKey(color);
        //? if <26.1 {
        ResourceLocation cached = COLOR_MASK_CACHE.get(cacheKey);
        //?} else {
        Identifier cached = COLOR_MASK_CACHE.get(cacheKey);
        //?}
        if (cached != null) {
            return cached;
        }

        //? if <1.20.6 {
        ResourceLocation generated = new ResourceLocation(texture.getNamespace(), "_color_mask/" + colorKey(color) + "/" + texture.getPath());
        //?} else {
        Identifier generated = Identifier.fromNamespaceAndPath(texture.getNamespace(), "_color_mask/" + colorKey(color) + "/" + texture.getPath());

        //?}
        NativeImage image = download(texture, NativeImageIO::copyImage);
        if (image == null) {
            return null;
        }
        applyColorMask(image, color);
        upload(generated, image);
        COLOR_MASK_CACHE.put(cacheKey, generated);
        return generated;
    }

    private static String colorKey(float[] color) {
        int r = Math.round(color[0] * 255.0F);
        int g = Math.round(color[1] * 255.0F);
        int b = Math.round(color[2] * 255.0F);
        return "%02x%02x%02x".formatted(r, g, b);
    }

    private static void applyColorMask(NativeImage image, float[] color) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                //? if <26.1 {
                int abgr = image.getPixelRGBA(x, y);
                //?} else {
                int abgr = image.getPixel(x, y);
                //?}
                int alpha = (abgr >>> 24) & 0xFF;
                int baseR = abgr & 0xFF;
                int baseG = (abgr >>> 8) & 0xFF;
                int baseB = (abgr >>> 16) & 0xFF;
                float mask = alpha / 255.0F;
                int r = maskedChannel(baseR, color[0], mask);
                int g = maskedChannel(baseG, color[1], mask);
                int b = maskedChannel(baseB, color[2], mask);
                int outputAlpha = alpha == 0 ? 0 : 0xFF;
                //? if <26.1 {
                image.setPixelRGBA(x, y, (outputAlpha << 24) | (b << 16) | (g << 8) | r);
                //?} else {
                image.setPixel(x, y, (outputAlpha << 24) | (b << 16) | (g << 8) | r);
                //?}
            }
        }
    }

    private static int maskedChannel(int base, float tint, float mask) {
        float tinted = base * tint;
        return Math.round(base + (tinted - base) * mask);
    }

    /**
     * 深拷贝 NativeImage，用于 download 后传递给外部持有（避免 try-with-resources 释放）。
     */
    public static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.format(), source.getWidth(), source.getHeight(), false);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                //? if <26.1 {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
                //?} else {
                copy.setPixel(x, y, source.getPixel(x, y));
                //?}
            }
        }
        return copy;
    }

    /**
     * 将 alpha 值二值化：有颜色内容的像素 → alpha 255，纯透明 → alpha 0。
     * 解决 Bedrock addon 纹理使用低 alpha（如 alpha=3）做边缘抗锯齿，
     * 以及部分像素（如眼睛）被错误保存为 alpha=0 但颜色非空的问题。
     */
    public void clampAlphaToBinary(NativeImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                //? if <26.1 {
                int rgba = image.getPixelRGBA(x, y);
                //?} else {
                int rgba = image.getPixel(x, y);
                //?}
                int alpha = (rgba >> 24) & 0xFF;
                if (alpha < 255) {
                    // 有颜色内容（任何 RGB 通道非零）→ 设为完全不透明
                    int rgb = rgba & 0x00FFFFFF;
                    if (rgb != 0 || alpha > 0) {
                        //? if <26.1 {
                        image.setPixelRGBA(x, y, rgba | 0xFF000000);
                        //?} else {
                        image.setPixel(x, y, rgba | 0xFF000000);
                        //?}
                    }
                }
            }
        }
    }
}
