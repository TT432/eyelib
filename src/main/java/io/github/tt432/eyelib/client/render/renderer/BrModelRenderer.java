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
import io.github.tt432.eyelib.util.math.PoseWrapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
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

        try (var last = PoseWrapper.from(poseStack.last())) {
            var pose = last.pose();
            pose.rotateY(R180);
            Matrix3f normal = last.normal();
            normal.rotateY(R180);
        }

        for (BrBone toplevelBone : model.toplevelBones()) {
            renderBone(renderParams, visitors, infos, toplevelBone);
        }

        poseStack.popPose();
        twoSideInfoMapStack.pop();
    }

    private static void renderBone(RenderParams renderParams, ModelRenderVisitorList visitors, BoneRenderInfos infos, BrBone bone) {
        renderParams.poseStack().pushPose();

        BoneRenderInfoEntry boneRenderInfoEntry = infos.get(bone.name());

        visitors.visitors().forEach(visitor -> visitor.visitBone(renderParams, bone, boneRenderInfoEntry, true));

        try (var last = PoseWrapper.from(renderParams.poseStack().last())) {
            var m4 = last.pose();

            m4.translate(boneRenderInfoEntry.getRenderPosition());

            Vector3f renderPivot = bone.pivot();

            m4.translate(renderPivot);

            Vector3f rotation = boneRenderInfoEntry.getRenderRotation();

            Matrix3f normal = last.normal();
            normal.rotateZYX(rotation);
            m4.rotateZYX(rotation);

            Vector3f boneRotation = bone.rotation();

            normal.rotateZYX(boneRotation);
            m4.rotateZYX(boneRotation);

            Vector3f scale = boneRenderInfoEntry.getRenderScala();

            last.pose().scale(scale.x, scale.y, scale.z);
            last.normal().scale(scale.x, scale.y, scale.z);

            m4.translate(renderPivot.negate(nPivot));
        }

        visitors.visitors().forEach(visitor -> visitor.visitBone(renderParams, bone, boneRenderInfoEntry, false));

        bone.locators().forEach((name, locator) -> {
            renderParams.poseStack().pushPose();

            try (var last1 = PoseWrapper.from(renderParams.poseStack().last())) {
                var pose = last1.pose();
                pose.translate(locator.getOffset());
                pose.rotateZYX(locator.getRotation());
                last1.normal().rotateZYX(locator.getRotation());
            }

            visitors.visitors().forEach(visitor -> visitor.visitLocator(renderParams, bone, name, locator, boneRenderInfoEntry));

            renderParams.poseStack().popPose();
        });

        for (int i = 0; i < bone.cubes().size(); i++) {
            BrCube brCube = bone.cubes().get(i);
            BrModelTextures.TwoSideInfoMap lastTwoSideInfoMap = twoSideInfoMapStack.getLast();
            renderCube(renderParams, visitors, brCube,
                    lastTwoSideInfoMap == null || lastTwoSideInfoMap.isTwoSide(bone.name(), i));
        }

        for (BrBone child : bone.children()) {
            renderBone(renderParams, visitors, infos, child);
        }

        renderParams.poseStack().popPose();
    }

    private static void renderCube(RenderParams renderParams, ModelRenderVisitorList visitors,
                                   BrCube cube, boolean needTwoSide) {
        visitors.visitors().forEach(visitor -> visitor.visitCube(renderParams, cube));

        for (BrFace face : cube.faces()) {
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
