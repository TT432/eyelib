package io.github.tt432.eyelib.testmod.client.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.render.sections.BlockEntitySectionGeometryRenderer;
import io.github.tt432.eyelib.client.render.sections.SectionGeometryRenderContext;
import io.github.tt432.eyelib.client.render.sections.cache.QuadListBakingVertexConsumer;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBufferIrisCompat;
import io.github.tt432.eyelib.client.render.sections.dynamic.DynamicChunkBuffers;
import io.github.tt432.eyelib.testmod.blocks.TestBenchBlockEntity;
import io.github.tt432.eyelib.util.EntryStreams;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.lighting.LightPipelineAwareModelBlockRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

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
