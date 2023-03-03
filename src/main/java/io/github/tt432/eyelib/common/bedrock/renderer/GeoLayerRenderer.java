package io.github.tt432.eyelib.common.bedrock.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.model.GeoModelProvider;
import io.github.tt432.eyelib.api.bedrock.renderer.GeoRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public abstract class GeoLayerRenderer<T extends Entity & Animatable> {
    protected final GeoRenderer<T> entityRenderer;

    public GeoLayerRenderer(GeoRenderer<T> entityRendererIn) {
        this.entityRenderer = entityRendererIn;
    }

    protected void renderCopyModel(GeoModelProvider<T> modelProvider,
                                   ResourceLocation texture, PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, T animatable, float partialTick, float red, float green, float blue) {
        if (!animatable.isInvisible()) {
            renderModel(modelProvider, texture, poseStack, bufferSource, packedLight, animatable,
                    partialTick, red, green, blue);
        }
    }

    protected void renderModel(GeoModelProvider<T> modelProvider,
                               ResourceLocation texture, PoseStack poseStack, MultiBufferSource bufferSource,
                               int packedLight, T animatable, float partialTick, float red, float green, float blue) {
        if (animatable instanceof LivingEntity entity) {
            RenderType renderType = getRenderType(texture);

            getRenderer().render(modelProvider.getModel(modelProvider.getModelLocation(animatable)),
                    animatable, partialTick, renderType, poseStack, bufferSource, bufferSource.getBuffer(renderType),
                    packedLight, LivingEntityRenderer.getOverlayCoords(entity, 0), red, green, blue, 1);
        }
    }

    public RenderType getRenderType(ResourceLocation textureLocation) {
        return RenderType.entityCutout(textureLocation);
    }

    public GeoModelProvider<T> getEntityModel() {
        return this.entityRenderer.getGeoModelProvider();
    }

    public GeoRenderer<T> getRenderer() {
        return this.entityRenderer;
    }

    protected ResourceLocation getEntityTexture(T entityIn) {
        return this.entityRenderer.getTextureLocation(entityIn);
    }

    public abstract void render(PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn,
                                T entityLivingBaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                                float netHeadYaw, float headPitch);
}