package io.github.tt432.eyelib.client.render.sections.compat.impl.embeddium;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;

/**
 * @author Argon4W
 */
public class EmbeddiumEntityTextureTerrainRenderPass extends TerrainRenderPass {
    private final ResourceLocation textureResourceLocation;

    public EmbeddiumEntityTextureTerrainRenderPass(RenderType renderType, boolean isTranslucent, ResourceLocation textureResourceLocation) {
        super(renderType, isTranslucent, true);
        this.textureResourceLocation = textureResourceLocation;
    }

    public AbstractTexture getTexture() {
        return Minecraft.getInstance().getTextureManager().getTexture(textureResourceLocation);
    }

    @Override
    public String toString() {
        return "[EntityTextureTerrainRenderPass: " + textureResourceLocation + "]";
    }
}
