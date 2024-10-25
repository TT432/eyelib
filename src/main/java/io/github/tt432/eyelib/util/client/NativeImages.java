package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author TT432
 */
@UtilityClass
public class NativeImages {
    @Nullable
    public <R> R downloadImage(ResourceLocation texture, Function<NativeImage, R> imageFunction) {
        Minecraft.getInstance().getTextureManager().getTexture(texture).bind();

        int[] width = new int[1];
        int[] height = new int[1];
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, width);
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, height);

        if (width[0] != 0 && height[0] != 0) {
            try (NativeImage nativeimage = new NativeImage(width[0], height[0], false)) {
                nativeimage.downloadTexture(0, false);
                return imageFunction.apply(nativeimage);
            }
        }

        return null;
    }

    public NativeImage loadImage(InputStream inputStream) throws IOException {
        var bufferedImage = ImageIO.read(inputStream);
        var image = new NativeImage(NativeImage.Format.RGBA,
                bufferedImage.getWidth(), bufferedImage.getHeight(), false);
        var w = bufferedImage.getWidth();
        var h = bufferedImage.getHeight();

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                image.setPixelRGBA(x, y, FastColor.ABGR32.fromArgb32(bufferedImage.getRGB(x, y)));
            }
        }

        return image;
    }
}
