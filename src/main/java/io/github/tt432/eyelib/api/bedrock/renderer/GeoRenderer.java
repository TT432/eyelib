package io.github.tt432.eyelib.api.bedrock.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.common.bedrock.model.element.Bone;
import io.github.tt432.eyelib.api.bedrock.model.GeoModelProvider;
import io.github.tt432.eyelib.common.AnimationTextureLoader;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.model.element.*;
import io.github.tt432.eyelib.molang.MolangDataSource;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.functions.utility.AddGlow;
import io.github.tt432.eyelib.util.Color;
import io.github.tt432.eyelib.util.RenderUtils;
import io.github.tt432.eyelib.util.math.Vec2d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface GeoRenderer<T> {
    MultiBufferSource getCurrentRTB();

    GeoModelProvider<T> getModelProvider();

    ResourceLocation getTextureLocation(T animatable);

    T getAnimatable();

    default ResourceLocation tryGetTexture(T animatable) {
        ResourceLocation textureLocation = getTextureLocation(animatable);

        if (AnimationTextureLoader.INSTANCE.has(textureLocation)) {
            TextureAtlasSprite sprite = AnimationTextureLoader.INSTANCE.get(textureLocation);
            textureLocation = sprite.atlas().location();
            AnimationData data = getData();

            data.putExtraData("tex_offset", new Vec2d(
                    sprite.getU0(),
                    sprite.getV0()
            ));

            data.putExtraData("tex_range", new Vec2d(
                    sprite.getU1() - sprite.getU0(),
                    sprite.getV1() - sprite.getV0()
            ));
        } else {
            clearTexData();
        }

        return textureLocation;
    }

    default void clearTexData() {
        AnimationData data = getData();
        data.putExtraData("tex_offset", new Vec2d(0, 0));
        data.putExtraData("tex_range", new Vec2d(1, 1));
    }

    default AnimationData getData() {
        return Objects.requireNonNullElse(MolangParser.getCurrentDataSource().getData(), AnimationData.EMPTY);
    }

    default void render(GeoModel model, T animatable, float partialTick, RenderType type, PoseStack poseStack,
                        @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                        int packedOverlay, float red, float green, float blue, float alpha) {
        setCurrentRTB(bufferSource);
        renderEarly(animatable, poseStack, partialTick, bufferSource, buffer, packedLight,
                packedOverlay, red, green, blue, alpha);

        if (bufferSource != null)
            buffer = bufferSource.getBuffer(type);

        renderLate(animatable, poseStack, partialTick, bufferSource, buffer, packedLight,
                packedOverlay, red, green, blue, alpha);
        // Render all top level bones
        for (Bone group : model.topLevelBones) {
            renderRecursively(group, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
        // Since we rendered at least once at this point, let's set the cycle to
        // repeated
        setCurrentModelRenderCycle(RenderCycle.RenderCycleImpl.REPEATED);
    }

    default void renderRecursively(Bone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                   int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);
        renderCubesOfBone(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        renderChildBones(bone, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }

    default void renderCubesOfBone(Bone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                   int packedOverlay, float red, float green, float blue, float alpha) {
        if (bone.isHidden())
            return;

        packedLight = getLight(packedLight, bone);

        List<GeoCube> childCubes = bone.childCubes.stream().sorted(Comparator.comparingDouble(cube -> {
            Entity cameraEntity = Minecraft.getInstance().cameraEntity;

            if (cameraEntity != null) {
                Vector3f pivot = cube.pivot;
                return cameraEntity.distanceToSqr(pivot.x(), pivot.y(), pivot.z());
            }

            return 0;
        })).toList();

        for (GeoCube cube : childCubes) {
            if (!bone.cubesAreHidden()) {
                poseStack.pushPose();
                renderCube(cube, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
                poseStack.popPose();
            }
        }
    }

    static int getLight(int sourceLight, Bone bone) {
        MolangDataSource currentDataSource = MolangParser.getCurrentDataSource();

        if (currentDataSource.get(Animatable.class) != null) {
            Map<String, Boolean> glowing = AddGlow.getGlowing();

            if (glowing.get(bone.name) != null && glowing.get(bone.name)) {
                return LightTexture.pack(15, 15);
            }
        }

        return sourceLight;
    }

    default void renderChildBones(Bone bone, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                  int packedOverlay, float red, float green, float blue, float alpha) {
        if (bone.childBonesAreHiddenToo())
            return;

        for (Bone childBone : bone.childBones) {
            renderRecursively(childBone, poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    default void renderCube(GeoCube cube, PoseStack poseStack, VertexConsumer buffer, int packedLight,
                            int packedOverlay, float red, float green, float blue, float alpha) {
        RenderUtils.translateToPivotPoint(poseStack, cube);
        RenderUtils.rotateMatrixAroundCube(poseStack, cube);
        RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
        Matrix3f normalisedPoseState = poseStack.last().normal();
        Matrix4f poseState = poseStack.last().pose();

        for (GeoQuad quad : cube.quads) {
            if (quad == null)
                continue;

            Vector3f normal = quad.normal.copy();

            normal.transform(normalisedPoseState);

            /*
             * Fix shading dark shading for flat cubes + compatibility wish Optifine shaders
             */
            if ((cube.size.y() == 0 || cube.size.z() == 0) && normal.x() < 0)
                normal.mul(-1, 1, 1);

            if ((cube.size.x() == 0 || cube.size.z() == 0) && normal.y() < 0)
                normal.mul(1, -1, 1);

            if ((cube.size.x() == 0 || cube.size.y() == 0) && normal.z() < 0)
                normal.mul(1, 1, -1);

            createVerticesOfQuad(quad, poseState, normal, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }

    default void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer,
                                      int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        AnimationData data = getData();
        Vec2d offset = data.<Vec2d>getExtraData("tex_offset").orElse(new Vec2d(0, 0));
        Vec2d range = data.<Vec2d>getExtraData("tex_range").orElse(new Vec2d(1, 1));

        for (GeoVertex vertex : quad.vertices) {
            Vector4f vector4f = new Vector4f(vertex.position.x(), vertex.position.y(), vertex.position.z(), 1);

            vector4f.transform(poseState);
            buffer.vertex(vector4f.x(), vector4f.y(), vector4f.z(), red, green, blue, alpha,
                    (float) (offset.getX() + range.getX() * vertex.getTextureU()),
                    (float) (offset.getY() + range.getY() * vertex.getTextureV()),
                    packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
        }
    }

    default void renderEarly(T animatable, PoseStack poseStack, float partialTick,
                             @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                             int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (getCurrentModelRenderCycle() == RenderCycle.RenderCycleImpl.INITIAL) {
            float width = getWidthScale(animatable);
            float height = getHeightScale(animatable);

            poseStack.scale(width, height, width);
        }
    }

    default void renderLate(T animatable, PoseStack poseStack, float partialTick, MultiBufferSource bufferSource,
                            VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue,
                            float alpha) {
    }

    default RenderType getRenderType(T animatable, float partialTick, PoseStack poseStack,
                                     @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight,
                                     ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    default Color getRenderColor(T animatable, float partialTick, PoseStack poseStack,
                                 @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, int packedLight) {
        return Color.WHITE;
    }

    default int getInstanceId(T animatable) {
        return animatable.hashCode();
    }

    default void setCurrentModelRenderCycle(RenderCycle cycle) {
    }

    @Nonnull
    default RenderCycle getCurrentModelRenderCycle() {
        return RenderCycle.RenderCycleImpl.INITIAL;
    }

    default void setCurrentRTB(MultiBufferSource bufferSource) {

    }

    default float getWidthScale(T animatable) {
        return 1F;
    }

    default float getHeightScale(T entity) {
        return 1F;
    }

    /**
     * Use {@link RenderUtils#prepMatrixForBone(PoseStack, Bone)}<br>
     * Remove in 1.20+
     */
    @Deprecated(forRemoval = true)
    default void preparePositionRotationScale(Bone bone, PoseStack poseStack) {
        RenderUtils.prepMatrixForBone(poseStack, bone);
    }
}
