package io.github.tt432.eyelib.mixin.compat.embeddium;

import io.github.tt432.eyelib.client.render.sections.compat.impl.embeddium.EmbeddiumCompatImpl;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBuffers;
import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.DefaultMaterials;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Argon4W
 */
@Pseudo
@Mixin(DefaultMaterials.class)
public class EmbeddiumDefaultMaterialsMixin {
    @Inject(method = "forRenderLayer", at = @At("HEAD"), cancellable = true)
    private static void forRenderLayer(RenderType layer, CallbackInfoReturnable<Material> cir) {
        if (DynamicChunkBuffers.DYNAMIC_CUTOUT_LAYERS.containsValue(layer)) {
            cir.setReturnValue(EmbeddiumCompatImpl.DYNAMIC_CUTOUT_MATERIALS.get(layer));
            return;
        }

        if (DynamicChunkBuffers.DYNAMIC_TRANSLUCENT_LAYERS.containsValue(layer)) {
            cir.setReturnValue(EmbeddiumCompatImpl.DYNAMIC_TRANSLUCENT_MATERIALS.get(layer));
        }
    }
}
