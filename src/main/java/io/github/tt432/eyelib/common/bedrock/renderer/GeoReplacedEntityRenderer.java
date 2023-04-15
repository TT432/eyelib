package io.github.tt432.eyelib.common.bedrock.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.ModelFetcherManager;
import io.github.tt432.eyelib.api.bedrock.renderer.GeoRenderer;
import io.github.tt432.eyelib.api.bedrock.renderer.RenderCycle;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.util.Color;
import io.github.tt432.eyelib.util.RenderUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.AvailableSince;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.tt432.eyelib.common.bedrock.renderer.GeoEntityRenderer.renderLeashPiece;

public abstract class GeoReplacedEntityRenderer<T extends Animatable> extends EntityRenderer implements GeoRenderer {
    protected static final Map<Class<? extends Animatable>, GeoReplacedEntityRenderer> renderers = new ConcurrentHashMap<>();

    static {
        ModelFetcherManager.addModelFetcher(a -> {
            GeoReplacedEntityRenderer renderer = renderers.get(a.getClass());
            return renderer == null ? null : renderer.getModelProvider();
        });
    }

    protected final AnimatedGeoModel<Animatable> modelProvider;
    @Getter
    protected T animatable;
    protected final List<GeoLayerRenderer> layerRenderers = new ObjectArrayList<>();
    protected Animatable currentAnimatable;
    protected float widthScale = 1;
    protected float heightScale = 1;
    protected Matrix4f dispatchedMat = new Matrix4f();
    protected Matrix4f renderEarlyMat = new Matrix4f();
    protected MultiBufferSource rtb = null;
    @Getter
    @Setter
    private RenderCycle currentModelRenderCycle = RenderCycle.RenderCycleImpl.INITIAL;

    protected GeoReplacedEntityRenderer(EntityRendererProvider.Context renderManager,
                                        AnimatedGeoModel<Animatable> modelProvider, T animatable) {
        super(renderManager);

        this.modelProvider = modelProvider;
        this.animatable = animatable;

        renderers.putIfAbsent(animatable.getClass(), this);
    }

    public static GeoReplacedEntityRenderer getRenderer(Class<? extends Animatable> animatableClass) {
        return renderers.get(animatableClass);
    }

    @AvailableSince(value = "3.1.24")
    @Override
    public float getWidthScale(Object animatable) {
        return this.widthScale;
    }

    @AvailableSince(value = "3.1.24")
    @Override
    public float getHeightScale(Object entity) {
        return this.heightScale;
    }

    @Override
    public void renderEarly(Object animatable, PoseStack poseStack, float partialTick,
                            MultiBufferSource bufferSource, VertexConsumer buffer, int packedLight, int packedOverlayIn,
                            float red, float green, float blue, float alpha) {
        this.renderEarlyMat = poseStack.last().pose().copy();
        GeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight, packedOverlayIn, red, green, blue, alpha);
    }

    @Override
    public void render(Entity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {

        render(entity, this.animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public void render(Entity entity, Animatable animatable, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (!(entity instanceof LivingEntity livingEntity))
            throw new IllegalStateException("Replaced renderer was not an instanceof LivingEntity");

        MolangVariableScope scope = animatable.getFactory().getScope();
        startRender(entity, animatable, poseStack, scope);
        scope.setValue("query.partial_tick", partialTick);

        this.currentAnimatable = animatable;
        this.dispatchedMat = poseStack.last().pose().copy();
        boolean shouldSit = entity.isPassenger() && (entity.getVehicle() != null && entity.getVehicle().shouldRiderSit());

        tryRenderLeash(entity, partialTick, poseStack, bufferSource);

        float lerpBodyRot = Mth.rotLerp(partialTick, livingEntity.yBodyRotO, livingEntity.yBodyRot);

        GeoEntityRenderer.applyRotations(livingEntity, poseStack, lerpBodyRot, partialTick);
        preRenderCallback(livingEntity, poseStack, partialTick);

        float lerpedAge = livingEntity.tickCount + partialTick;
        float limbSwingAmount = 0;
        float limbSwing = 0;

        if (!shouldSit && entity.isAlive()) {
            limbSwingAmount = Math.min(1, Mth.lerp(partialTick, livingEntity.animationSpeedOld, livingEntity.animationSpeed));
            limbSwing = livingEntity.animationPosition - livingEntity.animationSpeed * (1 - partialTick);

            if (livingEntity.isBaby())
                limbSwing *= 3.0F;
        }

        GeoModel model = this.modelProvider.getModel(this.modelProvider.getModelLocation(animatable));
        AnimationEvent predicate = new AnimationEvent(animatable, limbSwing, limbSwingAmount, partialTick,
                (limbSwingAmount <= -getSwingMotionAnimThreshold() || limbSwingAmount <= getSwingMotionAnimThreshold()),
                Collections.emptyList());

        this.modelProvider.setCustomAnimations(animatable, entity, getInstanceId(entity), predicate);

        ResourceLocation textureLocation = tryGetTexture(animatable);

        poseStack.translate(0, 0.01f, 0);
        RenderSystem.setShaderTexture(0, textureLocation);

        Color renderColor = getRenderColor(animatable, partialTick, poseStack, bufferSource, null, packedLight);
        RenderType renderType = getRenderType(entity, partialTick, poseStack, bufferSource, null, packedLight,
                textureLocation);

        assert Minecraft.getInstance().player != null;

        if (!entity.isInvisibleTo(Minecraft.getInstance().player)) {
            VertexConsumer glintBuffer = bufferSource.getBuffer(RenderType.entityGlintDirect());
            VertexConsumer translucentBuffer = bufferSource
                    .getBuffer(RenderType.entityTranslucentCull(textureLocation));
            render(model, entity, partialTick, renderType, poseStack, bufferSource,
                    glintBuffer != translucentBuffer ? VertexMultiConsumer.create(glintBuffer, translucentBuffer)
                            : null,
                    packedLight, getPackedOverlay(livingEntity, getOverlayProgress(livingEntity, partialTick)),
                    renderColor.getRed() / 255f, renderColor.getGreen() / 255f,
                    renderColor.getBlue() / 255f, renderColor.getAlpha() / 255f);
        }

        if (!entity.isSpectator()) {
            float netHeadYaw = (float) -scope.getValue("query.head_yaw_offset");
            float headPitch = (float) -scope.getValue("query.head_pitch_offset");

            for (GeoLayerRenderer layerRenderer : this.layerRenderers) {
                layerRenderer.render(poseStack, bufferSource, packedLight, entity, limbSwing, limbSwingAmount, partialTick,
                        lerpedAge, netHeadYaw, headPitch);
            }
        }

        finishRender(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight, scope);
    }

    private void finishRender(Entity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, MolangVariableScope scope) {
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        scope.removeValue("query.partial_tick");
        MolangParser.scopeStack.pop();
    }

    private void tryRenderLeash(Entity entity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (entity instanceof Mob mob) {
            Entity leashHolder = mob.getLeashHolder();

            if (leashHolder != null)
                renderLeash(mob, partialTick, poseStack, bufferSource, leashHolder);
        }
    }

    private void startRender(Entity entity, Animatable animatable, PoseStack poseStack, MolangVariableScope scope) {
        setCurrentModelRenderCycle(RenderCycle.RenderCycleImpl.INITIAL);
        poseStack.pushPose();
        MolangParser.scopeStack.push(scope);
        MolangParser.getCurrentDataSource().addSource(animatable, getInstanceId(entity));
        MolangParser.getCurrentDataSource().addSource(entity);
    }

    @Override
    public void renderRecursively(Bone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        if (bone.isTrackingXform()) {
            Entity entity = (Entity) this.animatable;
            Matrix4f poseState = poseStack.last().pose().copy();
            Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(poseState, this.dispatchedMat);

            bone.setModelSpaceXform(RenderUtils.invertAndMultiplyMatrices(poseState, this.renderEarlyMat));
            localMatrix.translate(new Vector3f(getRenderOffset(entity, 1)));
            bone.setLocalSpaceXform(localMatrix);

            Matrix4f worldState = localMatrix.copy();

            worldState.translate(new Vector3f(entity.position()));
            bone.setWorldSpaceXform(worldState);
        }

        GeoRenderer.super.renderRecursively(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue,
                alpha);
    }

    protected float getOverlayProgress(LivingEntity entity, float partialTicks) {
        return 0.0F;
    }

    @Override
    public int getInstanceId(Object animatable) {
        return ((Entity) animatable).getId();
    }

    protected void preRenderCallback(LivingEntity entity, PoseStack poseStack, float partialTick) {
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        return this.modelProvider.getTextureLocation(this.currentAnimatable);
    }

    @Override
    public AnimatedGeoModel getModelProvider() {
        return this.modelProvider;
    }

    /**
     * Use {@link GeoRenderer#getInstanceId(Object)}<br>
     * Remove in 1.20+
     */
    @Deprecated(forRemoval = true)
    public Integer getUniqueID(T animatable) {
        return getInstanceId(animatable);
    }

    public int getPackedOverlay(LivingEntity entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u),
                OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    @Override
    public boolean shouldShowName(Entity entity) {
        double nameRenderDistance = entity.isDiscrete() ? 32d : 64d;

        if (this.entityRenderDispatcher.distanceToSqr(entity) >= nameRenderDistance * nameRenderDistance)
            return false;

        return entity == this.entityRenderDispatcher.crosshairPickEntity && entity.hasCustomName() && Minecraft.renderNames();
    }

    protected float getSwingProgress(LivingEntity entity, float partialTick) {
        return entity.getAttackAnim(partialTick);
    }

    /**
     * Determines how far (from 0) the arm swing should be moving before counting as moving for animation purposes.
     */
    protected float getSwingMotionAnimThreshold() {
        return 0.15f;
    }

    @Override
    public ResourceLocation getTextureLocation(Object animatable) {
        return this.modelProvider.getTextureLocation((Animatable) animatable);
    }

    public final boolean addLayer(GeoLayerRenderer<? extends LivingEntity> layer) {
        return this.layerRenderers.add(layer);
    }

    public <E extends Entity> void renderLeash(Mob entity, float partialTick, PoseStack poseStack,
                                               MultiBufferSource bufferSource, E leashHolder) {
        double lerpBodyAngle = (Mth.lerp(partialTick, entity.yBodyRot, entity.yBodyRotO) * Mth.DEG_TO_RAD) + Mth.HALF_PI;
        Vec3 leashOffset = entity.getLeashOffset();
        double xAngleOffset = Math.cos(lerpBodyAngle) * leashOffset.z + Math.sin(lerpBodyAngle) * leashOffset.x;
        double zAngleOffset = Math.sin(lerpBodyAngle) * leashOffset.z - Math.cos(lerpBodyAngle) * leashOffset.x;
        double lerpOriginX = Mth.lerp(partialTick, entity.xo, entity.getX()) + xAngleOffset;
        double lerpOriginY = Mth.lerp(partialTick, entity.yo, entity.getY()) + leashOffset.y;
        double lerpOriginZ = Mth.lerp(partialTick, entity.zo, entity.getZ()) + zAngleOffset;
        Vec3 ropeGripPosition = leashHolder.getRopeHoldPosition(partialTick);
        float xDif = (float) (ropeGripPosition.x - lerpOriginX);
        float yDif = (float) (ropeGripPosition.y - lerpOriginY);
        float zDif = (float) (ropeGripPosition.z - lerpOriginZ);
        float offsetMod = Mth.fastInvSqrt(xDif * xDif + zDif * zDif) * 0.025f / 2f;
        float xOffset = zDif * offsetMod;
        float zOffset = xDif * offsetMod;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.leash());
        BlockPos entityEyePos = new BlockPos(entity.getEyePosition(partialTick));
        BlockPos holderEyePos = new BlockPos(leashHolder.getEyePosition(partialTick));
        int entityBlockLight = getBlockLightLevel(entity, entityEyePos);
        int holderBlockLight = leashHolder.isOnFire() ? 15 : leashHolder.level.getBrightness(LightLayer.BLOCK, holderEyePos);
        int entitySkyLight = entity.level.getBrightness(LightLayer.SKY, entityEyePos);
        int holderSkyLight = entity.level.getBrightness(LightLayer.SKY, holderEyePos);

        poseStack.pushPose();
        poseStack.translate(xAngleOffset, leashOffset.y, zAngleOffset);

        Matrix4f posMatrix = poseStack.last().pose();

        for (int segment = 0; segment <= 24; ++segment) {
            renderLeashPiece(vertexConsumer, posMatrix, xDif, yDif, zDif, entityBlockLight, holderBlockLight,
                    entitySkyLight, holderSkyLight, 0.025f, 0.025f, xOffset, zOffset, segment, false);
        }

        for (int segment = 24; segment >= 0; --segment) {
            renderLeashPiece(vertexConsumer, posMatrix, xDif, yDif, zDif, entityBlockLight, holderBlockLight,
                    entitySkyLight, holderSkyLight, 0.025f, 0.0f, xOffset, zOffset, segment, true);
        }

        poseStack.popPose();
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
