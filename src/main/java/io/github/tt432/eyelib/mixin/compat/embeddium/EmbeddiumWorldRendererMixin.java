package io.github.tt432.eyelib.mixin.compat.embeddium;

import io.github.tt432.eyelib.client.render.sections.compat.impl.embeddium.EmbeddiumCompatImpl;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBuffers;
import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer;
import org.embeddedt.embeddium.impl.render.chunk.ChunkRenderMatrices;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Argon4W
 */
@Pseudo
@Mixin(EmbeddiumWorldRenderer.class)
public class EmbeddiumWorldRendererMixin {
    @Shadow private RenderSectionManager renderSectionManager;

    @Inject(method = "drawChunkLayer", at = @At("HEAD"), cancellable = true)
    public void drawChunkLayer(RenderType renderLayer, Matrix4f normal, double x, double y, double z, CallbackInfo ci) {
        ChunkRenderMatrices matrices = ChunkRenderMatrices.from(normal);

        if (DynamicChunkBuffers.DYNAMIC_CUTOUT_LAYERS.containsValue(renderLayer)) {
            renderSectionManager.renderLayer(matrices, EmbeddiumCompatImpl.DYNAMIC_CUTOUT_PASSES.get(renderLayer), x, y, z);
            ci.cancel();
            return;
        }

        if (DynamicChunkBuffers.DYNAMIC_TRANSLUCENT_LAYERS.containsValue(renderLayer)) {
            renderSectionManager.renderLayer(matrices, EmbeddiumCompatImpl.DYNAMIC_TRANSLUCENT_PASSES.get(renderLayer), x, y, z);
            ci.cancel();
        }
    }
}
