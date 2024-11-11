package io.github.tt432.eyelib.mixin.compat.embeddium;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.tt432.eyelib.client.render.sections.compat.impl.embeddium.EmbeddiumCompatImpl;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegionManager;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author Argon4W
 */
@Pseudo
@Mixin(RenderRegionManager.class)
public abstract class EmbeddiumRenderRegionManagerMixin {
    @WrapOperation(method = "uploadMeshes(Lorg/embeddedt/embeddium/impl/gl/device/CommandList;Lorg/embeddedt/embeddium/impl/render/chunk/region/RenderRegion;Ljava/util/Collection;)V", at = @At(value = "FIELD", target = "Lorg/embeddedt/embeddium/impl/render/chunk/terrain/DefaultTerrainRenderPasses;ALL:[Lorg/embeddedt/embeddium/impl/render/chunk/terrain/TerrainRenderPass;"))
    public TerrainRenderPass[] wrapAllTerrainRenderPasses(Operation<TerrainRenderPass[]> original) {
        return ImmutableList.<TerrainRenderPass>builder().add(original.call()).addAll(EmbeddiumCompatImpl.DYNAMIC_CUTOUT_PASSES.values()).addAll(EmbeddiumCompatImpl.DYNAMIC_TRANSLUCENT_PASSES.values()).build().toArray(TerrainRenderPass[]::new);
    }
}
