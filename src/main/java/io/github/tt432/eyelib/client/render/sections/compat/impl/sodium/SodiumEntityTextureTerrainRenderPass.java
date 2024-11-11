package io.github.tt432.eyelib.client.render.sections.compat.impl.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * @author Argon4W
 */
public class SodiumEntityTextureTerrainRenderPass extends TerrainRenderPass {
    private final ResourceLocation textureResourceLocation;

    public SodiumEntityTextureTerrainRenderPass(RenderType renderType, boolean isTranslucent, ResourceLocation textureResourceLocation) {
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
