package io.github.tt432.eyelib.example.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.example.entity.BikeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import io.github.tt432.eyelib.example.client.model.entity.BikeModel;
import io.github.tt432.eyelib.common.bedrock.renderer.GeoEntityRenderer;

public class BikeGeoRenderer extends GeoEntityRenderer<BikeEntity> {
	public BikeGeoRenderer(EntityRendererProvider.Context renderManager) {
		super(renderManager, new BikeModel());
	}

	@Override
	public RenderType getRenderType(BikeEntity animatable, float partialTick, PoseStack poseStack,
									MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight,
									ResourceLocation texture) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
