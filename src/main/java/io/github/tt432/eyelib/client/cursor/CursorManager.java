package io.github.tt432.eyelib.client.cursor;

import io.github.tt432.eyelib.util.client.NativeImages;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * @author TT432
 */
public class CursorManager {
    public static Cursor load(ResourceLocation location, int xhot, int yhot) {
        return NativeImages.downloadImage(location,
                image -> {
                    ByteBuffer copy = MemoryUtil.memAlloc((int) image.size);
                    MemoryUtil.memCopy(image.pixels, MemoryUtil.memAddress(copy), image.size);
                    return new Cursor(
                            new GLFWImage(ByteBuffer.allocateDirect(GLFWImage.SIZEOF))
                                    .height(image.getHeight())
                                    .width(image.getWidth())
                                    .pixels(copy),
                            xhot,
                            yhot
                    );
                });
    }
}
