package io.github.tt432.eyelib.client.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.eyelibimporter.model.importer.ImportedImageData;
import io.github.tt432.eyelibutil.color.ColorEncodings;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

/** @author TT432 */
@UtilityClass
@NullMarked
public class NativeImageIO {
    public void upload(ResourceLocation texture, NativeImage image) {
        DynamicTexture dynamicTexture = new DynamicTexture(image);
        Minecraft.getInstance().getTextureManager().register(texture, dynamicTexture);
    }

    public void upload(String texture, NativeImage image) {
        upload(new ResourceLocation(texture), image);
    }

    @Nullable
    public <R> R download(ResourceLocation texture, Function<NativeImage, R> imageFunction) {
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
    }

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
                image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(bufferedImage.getRGB(x, y)));
            }
        }

        return image;
    }

    public NativeImage fromImportedImageData(ImportedImageData imageData) {
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, imageData.width(), imageData.height(), false);
        for (int x = 0; x < imageData.width(); x++) {
            for (int y = 0; y < imageData.height(); y++) {
                image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(imageData.getPixelArgb(x, y)));
            }
        }
        return image;
    }

    /**
     * 深拷贝 NativeImage，用于 download 后传递给外部持有（避免 try-with-resources 释放）。
     */
    public static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.format(), source.getWidth(), source.getHeight(), false);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
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
                int rgba = image.getPixelRGBA(x, y);
                int alpha = (rgba >> 24) & 0xFF;
                if (alpha < 255) {
                    // 有颜色内容（任何 RGB 通道非零）→ 设为完全不透明
                    int rgb = rgba & 0x00FFFFFF;
                    if (rgb != 0 || alpha > 0) {
                        image.setPixelRGBA(x, y, rgba | 0xFF000000);
                    }
                }
            }
        }
    }
}
