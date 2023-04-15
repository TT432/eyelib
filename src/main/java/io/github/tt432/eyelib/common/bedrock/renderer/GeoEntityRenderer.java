package io.github.tt432.eyelib.common.bedrock.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.ModelFetcherManager;
import io.github.tt432.eyelib.api.bedrock.model.GeoModelProvider;
import io.github.tt432.eyelib.api.bedrock.renderer.GeoRenderer;
import io.github.tt432.eyelib.api.bedrock.renderer.RenderCycle;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationEvent;
import io.github.tt432.eyelib.common.bedrock.model.AnimatedGeoModel;
import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import io.github.tt432.eyelib.common.bedrock.model.element.GeoModel;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.util.AnimationUtils;
import io.github.tt432.eyelib.util.Color;
import io.github.tt432.eyelib.util.RenderUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class GeoEntityRenderer<T extends LivingEntity & Animatable> extends EntityRenderer<T>
        implements GeoRenderer<T> {
    static {
        ModelFetcherManager.addModelFetcher(animatable -> animatable instanceof Entity entity ?
                (AnimatableModel<Animatable>) AnimationUtils.getGeoModelForEntity(entity) : null);
    }

    protected final AnimatedGeoModel<T> modelProvider;
    protected final List<GeoLayerRenderer<T>> layerRenderers = new ObjectArrayList<>();
    protected Matrix4f dispatchedMat = new Matrix4f();
    protected Matrix4f renderEarlyMat = new Matrix4f();
    @Getter
    protected T animatable;

    public MultiBufferSource rtb;
    public ResourceLocation whTexture;
    protected float widthScale = 1;
    protected float heightScale = 1;
    @Getter
    @Setter
    private RenderCycle currentModelRenderCycle = RenderCycle.RenderCycleImpl.INITIAL;

    protected GeoEntityRenderer(EntityRendererProvider.Context renderManager, AnimatedGeoModel<T> modelProvider) {
        super(renderManager);

        this.modelProvider = modelProvider;
    }

    @Override
    public void renderEarly(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue,
                            float partialTicks) {
        this.animatable = animatable;
        this.renderEarlyMat = poseStack.last().pose().copy();
        this.rtb = bufferSource;
        this.whTexture = getTextureLocation(animatable);

        GeoRenderer.super.renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight,
                packedOverlay, red, green, blue, partialTicks);
    }

    @Override
    public void render(T animatable, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight) {
        MolangVariableScope scope = animatable.getFactory().getScope();

        startRender(animatable, partialTick, poseStack);

        tryRenderLeash(animatable, partialTick, poseStack, bufferSource);

        this.dispatchedMat = poseStack.last().pose().copy();
        float lerpBodyRot = Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot);
        float ageInTicks = animatable.tickCount + partialTick;

        applyRotations(animatable, poseStack, lerpBodyRot, partialTick);

        float limbSwingAmount = 0;
        float limbSwing = 0;
        boolean shouldSit = animatable.isPassenger() && (animatable.getVehicle() != null && animatable.getVehicle().shouldRiderSit());

        if (!shouldSit && animatable.isAlive()) {
            limbSwingAmount = Mth.lerp(partialTick, animatable.animationSpeedOld, animatable.animationSpeed);
            limbSwing = animatable.animationPosition - animatable.animationSpeed * (1 - partialTick);

            if (animatable.isBaby())
                limbSwing *= 3f;

            if (limbSwingAmount > 1f)
                limbSwingAmount = 1f;
        }

        AnimationEvent<T> predicate = new AnimationEvent<>(animatable, limbSwing, limbSwingAmount, partialTick,
                (limbSwingAmount <= -getSwingMotionAnimThreshold() || limbSwingAmount > getSwingMotionAnimThreshold()),
                Collections.emptyList());
        GeoModel model = this.modelProvider.getModel(this.modelProvider.getModelLocation(animatable));

        this.modelProvider.setCustomAnimations(animatable, null, getInstanceId(animatable), predicate);

        ResourceLocation textureLocation = tryGetTexture(animatable);

        poseStack.translate(0, 0.01f, 0);
        RenderSystem.setShaderTexture(0, textureLocation);

        Color renderColor = getRenderColor(animatable, partialTick, poseStack, bufferSource, null, packedLight);
        RenderType renderType = getRenderType(animatable, partialTick, poseStack, bufferSource, null, packedLight,
                textureLocation);

        if (!animatable.isInvisibleTo(Minecraft.getInstance().player)) {
            VertexConsumer glintBuffer = bufferSource.getBuffer(RenderType.entityGlintDirect());
            VertexConsumer translucentBuffer = bufferSource
                    .getBuffer(RenderType.entityTranslucentCull(textureLocation));

            render(model, animatable, partialTick, renderType, poseStack, bufferSource,
                    glintBuffer != translucentBuffer ? VertexMultiConsumer.create(glintBuffer, translucentBuffer)
                            : null,
                    packedLight, getOverlay(animatable, 0), renderColor.getRed() / 255f,
                    renderColor.getGreen() / 255f, renderColor.getBlue() / 255f,
                    renderColor.getAlpha() / 255f);
        }

        if (!animatable.isSpectator()) {
            float netHeadYaw = (float) -scope.get("query.head_yaw_offset").evaluate(scope);
            float headPitch = (float) -scope.get("query.head_pitch_offset").evaluate(scope);

            for (GeoLayerRenderer<T> layerRenderer : this.layerRenderers) {
                layerRenderer.render(poseStack, bufferSource, packedLight, animatable, limbSwing, limbSwingAmount, partialTick, ageInTicks,
                        netHeadYaw, headPitch);
            }
        }

        finishRender(animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight, scope);
    }

    private void finishRender(T animatable, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, MolangVariableScope scope) {
        poseStack.popPose();
        super.render(animatable, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        GeoRenderers.renderPartialTick(scope);
        MolangParser.scopeStack.pop();
    }

    private static <T extends LivingEntity> void transForSleep(T animatable, PoseStack poseStack, float rotationYaw) {
        Direction bedDirection = animatable.getBedOrientation();

        if (bedDirection != null) {
            float eyePosOffset = animatable.getEyeHeight(Pose.STANDING) - 0.1F;

            poseStack.translate(-bedDirection.getStepX() * eyePosOffset, 0, -bedDirection.getStepZ() * eyePosOffset);
        }

        Direction bedOrientation = animatable.getBedOrientation();

        poseStack.mulPose(Vector3f.YP.rotationDegrees(bedOrientation != null ? getFacingAngle(bedOrientation) : rotationYaw));
        poseStack.mulPose(Vector3f.ZP.rotationDegrees(90f));
        poseStack.mulPose(Vector3f.YP.rotationDegrees(270f));
    }

    private void tryRenderLeash(T animatable, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (animatable instanceof Mob mob) {
            Entity leashHolder = mob.getLeashHolder();

            if (leashHolder != null)
                renderLeash(animatable, partialTick, poseStack, bufferSource, leashHolder);
        }
    }

    private void startRender(T animatable, float partialTick, PoseStack poseStack) {
        setCurrentModelRenderCycle(RenderCycle.RenderCycleImpl.INITIAL);
        poseStack.pushPose();
        MolangVariableScope scope = animatable.getFactory().getScope();
        MolangParser.scopeStack.push(scope);
        MolangParser.getCurrentDataSource().addSource(animatable, getInstanceId(animatable));
        GeoRenderers.setPartialTick(partialTick, scope);
    }

    @Override
    public void renderRecursively(Bone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);

        boolean rotOverride = bone.rotMat != null;

        if (rotOverride) {
            poseStack.last().pose().multiply(bone.rotMat);
            poseStack.last().normal().mul(new Matrix3f(bone.rotMat));
        } else {
            RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        }

        RenderUtils.scaleMatrixForBone(poseStack, bone);

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

        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        renderCubesOfBone(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);

        if (!bone.isHidden()) {
            renderChildBones(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }

        poseStack.popPose();
    }

    @Override
    public int getInstanceId(T animatable) {
        return animatable.getId();
    }

    @Override
    public GeoModelProvider<T> getModelProvider() {
        return this.modelProvider;
    }

    @Override
    public float getWidthScale(T animatable) {
        return this.widthScale;
    }

    @Override
    public float getHeightScale(T entity) {
        return this.heightScale;
    }

    public int getOverlay(T entity, float u) {
        return OverlayTexture.pack(OverlayTexture.u(u),
                OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    public static <T extends LivingEntity> void applyRotations(T animatable, PoseStack poseStack, float rotationYaw, float partialTick) {
        Pose pose = animatable.getPose();

        if (pose != Pose.SLEEPING) {
            poseStack.mulPose(Vector3f.YP.rotationDegrees(180f - rotationYaw));
        }

        if (animatable.deathTime > 0) {
            float deathRotation = (animatable.deathTime + partialTick - 1f) / 20f * 1.6f;

            poseStack.mulPose(Vector3f.ZP.rotationDegrees(Math.min(Mth.sqrt(deathRotation), 1) * 90f));
        } else if (animatable.isAutoSpinAttack()) {
            poseStack.mulPose(Vector3f.XP.rotationDegrees(-90f - animatable.getXRot()));
            poseStack.mulPose(Vector3f.YP.rotationDegrees((animatable.tickCount + partialTick) * -75f));
        } else if (pose == Pose.SLEEPING) {
            transForSleep(animatable, poseStack, rotationYaw);
        } else  {
            String name = null;

            if (animatable instanceof Player player) {
                if (!player.isModelPartShown(PlayerModelPart.CAPE))
                    return;

                name = animatable.getName().getString();
            } else if (animatable.hasCustomName()) {
                name = ChatFormatting.stripFormatting(animatable.getName().getString());
            }

            if (name != null && (name.equals("Dinnerbone") || name.equalsIgnoreCase("Grumm"))) {
                poseStack.translate(0, animatable.getBbHeight() + 0.1f, 0);
                poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
            }
        }
    }

    private static float getFacingAngle(Direction direction) {
        return switch (direction) {
            case SOUTH -> 90f;
            case NORTH -> 270f;
            case EAST -> 180f;
            default -> 0f;
        };
    }

    @Override
    public boolean shouldShowName(T animatable) {
        double nameRenderDistance = animatable.isDiscrete() ? 32d : 64d;

        if (this.entityRenderDispatcher.distanceToSqr(animatable) >= nameRenderDistance * nameRenderDistance)
            return false;

        return animatable == this.entityRenderDispatcher.crosshairPickEntity && animatable.hasCustomName() && Minecraft.renderNames();
    }

    /**
     * Determines how far (from 0) the arm swing should be moving before counting as moving for animation purposes.
     */
    protected float getSwingMotionAnimThreshold() {
        return 0.15f;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T animatable) {
        return this.modelProvider.getTextureLocation(animatable);
    }

    public final boolean addLayer(GeoLayerRenderer<T> layer) {
        return this.layerRenderers.add(layer);
    }

    public <E extends Entity> void renderLeash(T entity, float partialTick, PoseStack poseStack,
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
            GeoEntityRenderer.renderLeashPiece(vertexConsumer, posMatrix, xDif, yDif, zDif, entityBlockLight, holderBlockLight,
                    entitySkyLight, holderSkyLight, 0.025f, 0.025f, xOffset, zOffset, segment, false);
        }

        for (int segment = 24; segment >= 0; --segment) {
            GeoEntityRenderer.renderLeashPiece(vertexConsumer, posMatrix, xDif, yDif, zDif, entityBlockLight, holderBlockLight,
                    entitySkyLight, holderSkyLight, 0.025f, 0.0f, xOffset, zOffset, segment, true);
        }

        poseStack.popPose();
    }

    public static void renderLeashPiece(VertexConsumer buffer, Matrix4f positionMatrix, float xDif, float yDif,
                                         float zDif, int entityBlockLight, int holderBlockLight, int entitySkyLight,
                                         int holderSkyLight, float width, float yOffset, float xOffset, float zOffset,
                                         int segment, boolean isLeashKnot) {
        float piecePosPercent = segment / 24f;
        int lerpBlockLight = (int) Mth.lerp(piecePosPercent, entityBlockLight, holderBlockLight);
        int lerpSkyLight = (int) Mth.lerp(piecePosPercent, entitySkyLight, holderSkyLight);
        int packedLight = LightTexture.pack(lerpBlockLight, lerpSkyLight);
        float knotColourMod = segment % 2 == (isLeashKnot ? 1 : 0) ? 0.7f : 1f;
        float red = 0.5f * knotColourMod;
        float green = 0.4f * knotColourMod;
        float blue = 0.3f * knotColourMod;
        float x = xDif * piecePosPercent;
        float y = yDif > 0.0f ? yDif * piecePosPercent * piecePosPercent : yDif - yDif * (1.0f - piecePosPercent) * (1.0f - piecePosPercent);
        float z = zDif * piecePosPercent;

        buffer.vertex(positionMatrix, x - xOffset, y + yOffset, z + zOffset).color(red, green, blue, 1).uv2(packedLight).endVertex();
        buffer.vertex(positionMatrix, x + xOffset, y + width - yOffset, z - zOffset).color(red, green, blue, 1).uv2(packedLight).endVertex();
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
