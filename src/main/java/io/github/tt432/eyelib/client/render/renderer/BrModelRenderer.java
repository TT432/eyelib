package io.github.tt432.eyelib.client.render.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.BrModelTextures;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrModelRenderer {
    private static final Vector3f nPivot = new Vector3f();
    private static final float R180 = 180 * EyeMath.DEGREES_TO_RADIANS;
    private static final Deque<BrModelTextures.TwoSideInfoMap> twoSideInfoMapStack = new ArrayDeque<>();

    public static void render(RenderParams renderParams, BrModel model, BoneRenderInfos infos,
                              @Nullable BrModelTextures.TwoSideInfoMap map, ModelRenderVisitorList visitors) {
        twoSideInfoMapStack.push(map);
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.rotateY(R180);
        Matrix3f normal = last.normal();
        normal.rotateY(R180);

        visitors.visitors().forEach(v -> v.visitModel(renderParams));

        for (BrBone toplevelBone : model.toplevelBones()) {
            renderBone(renderParams, visitors, infos, toplevelBone);
        }

        poseStack.popPose();
        twoSideInfoMapStack.pop();
    }

    private static void renderBone(RenderParams renderParams, ModelRenderVisitorList visitors, BoneRenderInfos infos, BrBone bone) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        BoneRenderInfoEntry boneRenderInfoEntry = infos.get(bone.name());

        applyBoneTranslate(bone, poseStack, boneRenderInfoEntry);

        visitors.visitors().forEach(visitor -> visitor.visitBone(renderParams, bone, boneRenderInfoEntry));

        visitLocators(renderParams, visitors, bone, poseStack, boneRenderInfoEntry);

        for (int i = 0; i < bone.cubes().size(); i++) {
            BrModelTextures.TwoSideInfoMap lastTwoSideInfoMap = twoSideInfoMapStack.getLast();
            renderCube(renderParams, visitors, bone.cubes().get(i),
                    lastTwoSideInfoMap == null || lastTwoSideInfoMap.isTwoSide(bone.name(), i));
        }

        for (BrBone child : bone.children()) {
            renderBone(renderParams, visitors, infos, child);
        }

        poseStack.popPose();
    }

    private static void visitLocators(RenderParams renderParams, ModelRenderVisitorList visitors, BrBone bone, PoseStack poseStack, BoneRenderInfoEntry boneRenderInfoEntry) {
        bone.locators().forEach((name, locator) -> {
            poseStack.pushPose();

            PoseStack.Pose last1 = poseStack.last();
            Matrix4f pose = last1.pose();
            pose.translate(locator.offset());
            pose.rotateZYX(locator.rotation());
            last1.normal().rotateZYX(locator.rotation());

            visitors.visitors().forEach(visitor -> visitor.visitLocator(renderParams, bone, name, locator, boneRenderInfoEntry));

            poseStack.popPose();
        });
    }

    private static void applyBoneTranslate(BrBone bone, PoseStack poseStack, BoneRenderInfoEntry boneRenderInfoEntry) {
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();

        pose.translate(boneRenderInfoEntry.getRenderPosition());

        Vector3f renderPivot = bone.pivot();
        pose.translate(renderPivot);

        applyBoneRotation(bone, boneRenderInfoEntry, last);

        Vector3f scale = boneRenderInfoEntry.getRenderScala();
        poseStack.scale(scale.x, scale.y, scale.z);

        pose.translate(renderPivot.negate(nPivot));
    }

    private static void applyBoneRotation(BrBone bone, BoneRenderInfoEntry boneRenderInfoEntry, PoseStack.Pose last) {
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        Vector3f rotation = boneRenderInfoEntry.getRenderRotation();

        normal.rotateZYX(rotation);
        pose.rotateZYX(rotation);

        Vector3f boneRotation = bone.rotation();

        normal.rotateZYX(boneRotation);
        pose.rotateZYX(boneRotation);
    }

    private static void renderCube(RenderParams renderParams, ModelRenderVisitorList visitors,
                                   BrCube cube, boolean needTwoSide) {
        visitors.visitors().forEach(visitor -> visitor.visitCube(renderParams, cube));

        for (BrFace face : cube.faces()) {
            visitors.visitors().forEach(visitor -> visitor.visitFace(renderParams, cube, face));

            for (int i = 0; i < face.getVertex().length; i++) {
                int finalI = i;
                visitors.visitors().forEach(visitor -> visitor.visitVertex(renderParams, cube, face, finalI));
            }

            if (needTwoSide) {
                for (int i = face.getVertex().length - 1; i >= 0; i--) {
                    int finalI = i;
                    visitors.visitors().forEach(visitor -> visitor.visitVertex(renderParams, cube, face, finalI));
                }
            }
        }
    }
}
