package io.github.tt432.eyelib.client.material;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.client.gl.BlendFactor;
import io.github.tt432.eyelib.client.gl.DepthFunc;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * @author TT432
 */
public interface Material {
    VertexFormat getFormat();

    RenderType getRenderType(ResourceLocation texture);

    DepthFunc depthFunc();

    BlendFactor blendSrc();

    BlendFactor blendDst();

    BlendFactor alphaSrc();

    BlendFactor alphaDst();

    default void open() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(depthFunc().value);
    }

    default void close() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }
}
