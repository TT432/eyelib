package io.github.tt432.eyelib.example.client.renderer.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.common.bedrock.renderer.GeoBlockRenderer;
import io.github.tt432.eyelib.example.block.tile.FertilizerTileEntity;
import io.github.tt432.eyelib.example.client.model.tile.FertilizerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class FertilizerTileRenderer extends GeoBlockRenderer<FertilizerTileEntity> {
    public FertilizerTileRenderer(BlockEntityRendererProvider.Context rendererDispatcherIn) {
        super(rendererDispatcherIn, new FertilizerModel());
    }

    @Override
    public RenderType getRenderType(FertilizerTileEntity animatable, float partialTick, PoseStack poseStack,
                                    MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight,
                                    ResourceLocation texture) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
