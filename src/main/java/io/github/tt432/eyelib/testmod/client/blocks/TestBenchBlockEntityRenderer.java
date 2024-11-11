package io.github.tt432.eyelib.testmod.client.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.render.sections.BlockEntitySectionGeometryRenderer;
import io.github.tt432.eyelib.client.render.sections.SectionGeometryRenderContext;
import io.github.tt432.eyelib.testmod.blocks.TestBenchBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;

public class TestBenchBlockEntityRenderer implements BlockEntityRenderer<TestBenchBlockEntity>, BlockEntitySectionGeometryRenderer<TestBenchBlockEntity> {
    @Override
    public void renderSectionGeometry(TestBenchBlockEntity blockEntity, AddSectionGeometryEvent.SectionRenderingContext context, PoseStack poseStack, BlockPos pos, BlockPos regionOrigin, SectionGeometryRenderContext renderAndCacheContext) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        renderAndCacheContext.renderCachedEntity(EntityType.BOGGED.create(blockEntity.getLevel()), ResourceLocation.withDefaultNamespace("bogged_cache"), poseStack);
        poseStack.popPose();
    }

    @Override
    public void render(TestBenchBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

    }
}
