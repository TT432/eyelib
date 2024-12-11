package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author TT432
 */
@UtilityClass
public class Textures {
    /**
     * @param textures textures
     * @return 合并后的图片，需要手动释放
     */
    public NativeImage layerMerging(List<ResourceLocation> textures) {
        AtomicReference<NativeImage> image = new AtomicReference<>();

        int maxX = 16;
        int maxY = 16;

        int[] width = new int[1];
        int[] height = new int[1];

        for (int i = textures.size() - 1; i >= 0; i--) {
            ResourceLocation resourceLocation = textures.get(i);
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
            if (texture == MissingTextureAtlasSprite.getTexture()) continue;
            texture.bind();
            glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
            glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);

            if (width[0] > maxX) maxX = width[0];

            if (height[0] > maxY) maxY = height[0];
        }

        NativeImage newValue = new NativeImage(maxX, maxY, false);
        image.set(newValue);
        BufferUtils.zeroBuffer(MemoryUtil.memIntBuffer(newValue.pixels, newValue.getWidth() * newValue.getHeight()));

        for (int i = textures.size() - 1; i >= 0; i--) {
            ResourceLocation resourceLocation = textures.get(i);
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(resourceLocation);
            if (texture == MissingTextureAtlasSprite.getTexture()) continue;

            NativeImages.downloadImage(resourceLocation, ni -> {
                NativeImage nativeImage = image.get();

                int width1 = nativeImage.getWidth();
                int height1 = nativeImage.getHeight();

                int width2 = ni.getWidth();
                int height2 = ni.getHeight();

                int sw = width2 / width1;
                int sh = height2 / height1;

                for (int x = 0; x < width1; x++) {
                    for (int y = 0; y < height1; y++) {
                        nativeImage.setPixelRGBA(x, y, blendPixels(
                                ni.getPixelRGBA(x * sw, y * sh),
                                nativeImage.getPixelRGBA(x, y)
                        ));
                    }
                }

                return null;
            });
        }

        return image.get();
    }

    public static int blendPixels(int src, int dst) {
        if ((dst & 0xFFFFFF) == 0 || (dst & 0xFF000000) == 0) return src;
        if ((src & 0xFFFFFF) == 0 || (src & 0xFF000000) == 0) return dst;

        int a1 = (src >> 24) & 0xFF;
        int r1 = (src >> 16) & 0xFF;
        int g1 = (src >> 8) & 0xFF;
        int b1 = src & 0xFF;

        int a2 = (dst >> 24) & 0xFF;
        int r2 = (dst >> 16) & 0xFF;
        int g2 = (dst >> 8) & 0xFF;
        int b2 = dst & 0xFF;

        int alpha = a1 + (a2 * (255 - a1) / 255);

        int r = (r1 * a1 / 255) + (r2 * (255 - a1) / 255);
        int g = (g1 * a1 / 255) + (g2 * (255 - a1) / 255);
        int b = (b1 * a1 / 255) + (b2 * (255 - a1) / 255);

        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }
}
