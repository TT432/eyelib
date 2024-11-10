package io.github.tt432.eyelib.mixin.compat.embeddium;

import io.github.tt432.eyelib.client.render.sections.compat.impl.embeddium.EmbeddiumEntityTextureTerrainRenderPass;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderOptions;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderTextureSlot;
import org.embeddedt.embeddium.impl.render.chunk.shader.ShaderBindingContext;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Argon4W
 */
@Pseudo
@Mixin(ChunkShaderInterface.class)
public abstract class EmbeddiumChunkShaderInterfaceMixin {
    @Shadow @Deprecated(
        forRemoval = true
    ) protected abstract void bindTexture(ChunkShaderTextureSlot slot, int textureId);

    @Unique
    private TerrainRenderPass eyelib$terrainRenderPass;

    @Inject(method = "<init>", at = @At("TAIL"))
    public void constructor(ShaderBindingContext context, ChunkShaderOptions options, CallbackInfo ci) {
        eyelib$terrainRenderPass = options.pass();
    }

    @Inject(method = "setupState", at = @At("TAIL"))
    public void setupState(CallbackInfo ci) {
        if (eyelib$terrainRenderPass instanceof EmbeddiumEntityTextureTerrainRenderPass pass) {
            bindTexture(ChunkShaderTextureSlot.BLOCK, pass.getTexture().getId());
        }
    }
}
