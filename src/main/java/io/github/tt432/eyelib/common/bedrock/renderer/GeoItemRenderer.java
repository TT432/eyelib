package io.github.tt432.eyelib.common.bedrock.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.ModelFetcherManager;
import io.github.tt432.eyelib.api.bedrock.renderer.GeoRenderer;
import io.github.tt432.eyelib.api.bedrock.renderer.RenderCycle;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.util.Color;
import io.github.tt432.eyelib.util.GeckoLibUtil;
import io.github.tt432.eyelib.util.RenderUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Matrix4f;

import java.util.Collections;

public abstract class GeoItemRenderer<T extends Item & Animatable> extends BlockEntityWithoutLevelRenderer
        implements GeoRenderer<T> {
    // Register a model fetcher for this renderer
    static {
        ModelFetcherManager.addModelFetcher(animatable -> {
            if (animatable instanceof Item item
                    && IClientItemExtensions.of(item).getCustomRenderer() instanceof GeoItemRenderer geoItemRenderer)
                return (AnimatableModel<Animatable>) geoItemRenderer.getModelProvider();

            return null;
        });
    }

    @Getter
    @Setter
    protected AnimatedGeoModel<T> modelProvider;
    protected ItemStack currentItemStack;
    protected float widthScale = 1;
    protected float heightScale = 1;
    protected Matrix4f dispatchedMat = new Matrix4f();
    protected Matrix4f renderEarlyMat = new Matrix4f();
    @Getter
    protected T animatable;
    protected MultiBufferSource rtb = null;

    @Setter
    @Getter
    private RenderCycle currentModelRenderCycle = RenderCycle.RenderCycleImpl.INITIAL;

    protected GeoItemRenderer(AnimatedGeoModel<T> modelProvider) {
        this(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels(),
                modelProvider);
    }

    protected GeoItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet,
                           AnimatedGeoModel<T> modelProvider) {
        super(dispatcher, modelSet);

        this.modelProvider = modelProvider;
    }

    @Override
    public float getWidthScale(T animatable) {
        return this.widthScale;
    }

    @Override
    public float getHeightScale(T entity) {
        return this.heightScale;
    }

    // fixes the item lighting
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (transformType == ItemDisplayContext.GUI) {
            poseStack.pushPose();
            MultiBufferSource.BufferSource defaultBufferSource = bufferSource instanceof MultiBufferSource.BufferSource bufferSource2
                    ? bufferSource2
                    : Minecraft.getInstance().renderBuffers().bufferSource();
            Lighting.setupForFlatItems();
            render((T) stack.getItem(), poseStack, bufferSource, packedLight, stack);
            defaultBufferSource.endBatch();
            RenderSystem.enableDepthTest();
            Lighting.setupFor3DItems();
            poseStack.popPose();
        } else {
            this.render((T) stack.getItem(), poseStack, bufferSource, packedLight, stack);
        }
    }

    public void render(T animatable, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       ItemStack stack) {
        this.currentItemStack = stack;
        GeoModel model = this.modelProvider.getModel(this.modelProvider.getModelLocation(animatable));
        AnimationEvent animationEvent = new AnimationEvent(animatable, 0, 0, Minecraft.getInstance().getFrameTime(),
                false, Collections.singletonList(stack));
        this.dispatchedMat = new Matrix4f(poseStack.last().pose());

        setCurrentModelRenderCycle(RenderCycle.RenderCycleImpl.INITIAL);
        this.modelProvider.setCustomAnimations(animatable, null, getInstanceId(animatable), animationEvent);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0.51f, 0.5f);

        RenderSystem.setShaderTexture(0, getTextureLocation(animatable));
        Color renderColor = getRenderColor(animatable, 0, poseStack, bufferSource, null, packedLight);
        RenderType renderType = getRenderType(animatable, 0, poseStack, bufferSource, null, packedLight,
                getTextureLocation(animatable));
        render(model, animatable, 0, renderType, poseStack, bufferSource, null, packedLight, OverlayTexture.NO_OVERLAY,
                renderColor.getRed() / 255f, renderColor.getGreen() / 255f, renderColor.getBlue() / 255f,
                renderColor.getAlpha() / 255f);
        poseStack.popPose();
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            VertexConsumer buffer, int packedLight, int packedOverlayIn, float red, float green, float blue,
                            float alpha) {
        this.renderEarlyMat = new Matrix4f(poseStack.last().pose());
        this.animatable = animatable;

        GeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight,
                packedOverlayIn, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(Bone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        if (bone.isTrackingXform()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(poseState, this.dispatchedMat);

            bone.setModelSpaceXform(RenderUtils.invertAndMultiplyMatrices(poseState, this.renderEarlyMat));
            localMatrix.translate(getRenderOffset(this.animatable, 1).toVector3f());
            bone.setLocalSpaceXform(localMatrix);
        }

        GeoRenderer.super.renderRecursively(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue,
                alpha);
    }

    public Vec3 getRenderOffset(T animatable, float partialTick) {
        return Vec3.ZERO;
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return this.modelProvider.getTextureLocation(animatable);
    }

    @Override
    public int getInstanceId(T animatable) {
        return GeckoLibUtil.getIDFromStack(this.currentItemStack);
    }

    @Override
    public void setCurrentRTB(MultiBufferSource bufferSource) {
        this.rtb = bufferSource;
    }

    @Override
    public MultiBufferSource getCurrentRTB() {
        return this.rtb;
    }
}
