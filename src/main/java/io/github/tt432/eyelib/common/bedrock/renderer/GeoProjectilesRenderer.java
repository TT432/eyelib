package io.github.tt432.eyelib.common.bedrock.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.model.GeoModelProvider;
import io.github.tt432.eyelib.api.bedrock.renderer.GeoRenderer;
import io.github.tt432.eyelib.api.bedrock.renderer.RenderCycle;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationController;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoBone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.util.AnimationUtils;
import io.github.tt432.eyelib.util.Color;
import io.github.tt432.eyelib.util.RenderUtils;
import io.github.tt432.eyelib.util.data.EntityModelData;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus.AvailableSince;

import javax.annotation.Nonnull;
import java.util.Collections;

public class GeoProjectilesRenderer<T extends Entity & Animatable> extends EntityRenderer<T>
        implements GeoRenderer<T> {
    static {
        AnimationController.addModelFetcher(animatable -> animatable instanceof Entity entity ?
                (AnimatableModel<Animatable>) AnimationUtils.getGeoModelForEntity(entity) : null);
    }

    protected final AnimatedGeoModel<T> modelProvider;
    protected float widthScale = 1;
    protected float heightScale = 1;
    protected Matrix4f dispatchedMat = new Matrix4f();
    protected Matrix4f renderEarlyMat = new Matrix4f();
    @Getter
    protected T animatable;
    private RenderCycle currentModelRenderCycle = RenderCycle.RenderCycleImpl.INITIAL;
    protected MultiBufferSource rtb = null;

    public GeoProjectilesRenderer(EntityRendererProvider.Context renderManager, AnimatedGeoModel<T> modelProvider) {
        super(renderManager);

        this.modelProvider = modelProvider;
    }

    @Override
    public void render(T animatable, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        GeoModel model = this.modelProvider.getModel(modelProvider.getModelLocation(animatable));
        this.dispatchedMat = poseStack.last().pose().copy();

        setCurrentModelRenderCycle(RenderCycle.RenderCycleImpl.INITIAL);
        poseStack.pushPose();
        poseStack.mulPose(Vector3f.YP.rotationDegrees(Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) - 90));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));

        AnimationEvent<T> predicate = new AnimationEvent<T>(animatable, 0, 0, partialTick,
                false, Collections.singletonList(new EntityModelData()));

        modelProvider.setCustomAnimations(animatable, null, getInstanceId(animatable), predicate);
        RenderSystem.setShaderTexture(0, getTextureLocation(animatable));

        Color renderColor = getRenderColor(animatable, partialTick, poseStack, bufferSource, null, packedLight);
        RenderType renderType = getRenderType(animatable, partialTick, poseStack, bufferSource, null, packedLight,
                getTextureLocation(animatable));

        if (!animatable.isInvisibleTo(Minecraft.getInstance().player)) {
            render(model, animatable, partialTick, renderType, poseStack, bufferSource, null, packedLight,
                    getPackedOverlay(animatable, 0), renderColor.getRed() / 255f, renderColor.getGreen() / 255f,
                    renderColor.getBlue() / 255f, renderColor.getAlpha() / 255f);
        }

        poseStack.popPose();
        super.render(animatable, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue,
                            float alpha) {
        this.renderEarlyMat = poseStack.last().pose().copy();
        this.animatable = animatable;

        GeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer,
                packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void renderRecursively(GeoBone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        if (bone.isTrackingXform()) {
            Matrix4f poseState = poseStack.last().pose().copy();
            Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(poseState, this.dispatchedMat);

            bone.setModelSpaceXform(RenderUtils.invertAndMultiplyMatrices(poseState, this.renderEarlyMat));
            localMatrix.translate(new Vector3f(getRenderOffset(this.animatable, 1)));
            bone.setLocalSpaceXform(localMatrix);

            Matrix4f worldState = localMatrix.copy();

            worldState.translate(new Vector3f(this.animatable.position()));
            bone.setWorldSpaceXform(worldState);
        }

        GeoRenderer.super.renderRecursively(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public int getPackedOverlay(Entity entity, float uIn) {
        return OverlayTexture.pack(OverlayTexture.u(uIn), OverlayTexture.v(false));
    }

    @Override
    public GeoModelProvider<T> getGeoModelProvider() {
        return this.modelProvider;
    }

    @AvailableSince(value = "3.1.24")
    @Override
    @Nonnull
    public RenderCycle getCurrentModelRenderCycle() {
        return this.currentModelRenderCycle;
    }

    @AvailableSince(value = "3.1.24")
    @Override
    public void setCurrentModelRenderCycle(RenderCycle currentModelRenderCycle) {
        this.currentModelRenderCycle = currentModelRenderCycle;
    }

    @AvailableSince(value = "3.1.24")
    @Override
    public float getWidthScale(T animatable) {
        return this.widthScale;
    }

    @AvailableSince(value = "3.1.24")
    @Override
    public float getHeightScale(T entity) {
        return this.heightScale;
    }

    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return this.modelProvider.getTextureLocation(animatable);
    }

    /**
     * Use {@link GeoRenderer#getInstanceId(Object)}<br>
     * Remove in 1.20+
     */
    @Deprecated(forRemoval = true)
    public Integer getUniqueID(T animatable) {
        return getInstanceId(animatable);
    }

    @Override
    public int getInstanceId(T animatable) {
        return animatable.getUUID().hashCode();
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
