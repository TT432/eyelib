package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfoEntry;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.client.render.visitor.BrModelRenderVisitor;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BrModelRenderer {
    private static final Vector3f nPivot = new Vector3f();
    private static final Quaternionf tQ = new Quaternionf();

    public static void render(BrModel model, BoneRenderInfos infos, PoseStack poseStack, VertexConsumer consumer, BrModelRenderVisitor visitor) {
        poseStack.pushPose();

        for (BrBone toplevelBone : model.toplevelBones()) {
            renderBone(poseStack,visitor, infos, toplevelBone, consumer);
        }

        poseStack.popPose();
    }

    private static void renderBone(PoseStack poseStack, BrModelRenderVisitor visitor, BoneRenderInfos infos, BrBone bone, VertexConsumer consumer) {
        poseStack.pushPose();

        BoneRenderInfoEntry boneRenderInfoEntry = infos.get(bone);

        visitor.visitBone(poseStack, bone, boneRenderInfoEntry, consumer, true);

        Matrix4f m4 = poseStack.last().pose();

        m4.translate(boneRenderInfoEntry.getRenderPosition());

        Vector3f renderPivot = bone.pivot();

        m4.translate(renderPivot);

        Vector3f rotation = boneRenderInfoEntry.getRenderRotation();

        tQ.rotationZYX(rotation.z, rotation.y, rotation.x);
        poseStack.mulPose(tQ);

        Vector3f scale = boneRenderInfoEntry.getRenderScala();

        poseStack.scale(scale.x, scale.y, scale.z);

        m4.translate(renderPivot.negate(nPivot));

        visitor.visitBone(poseStack, bone, boneRenderInfoEntry, consumer, false);

        for (BrCube cube : bone.cubes()) {
            renderCube(poseStack, visitor, cube, consumer);
        }

        for (BrBone child : bone.children()) {
            renderBone(poseStack, visitor, infos, child, consumer);
        }

       poseStack.popPose();
    }

    private static void renderCube(PoseStack poseStack, BrModelRenderVisitor visitor, BrCube cube, VertexConsumer consumer) {
        visitor.visitCube(poseStack, cube, consumer);

        for (BrFace face : cube.faces()) {
            for (int i = 0; i < face.getVertex().length; i++) {
                visitor.visitVertex(poseStack, cube, face, i, consumer);
            }

            for (int i = face.getVertex().length - 1; i >= 0; i--) {
                visitor.visitVertex(poseStack, cube, face, i, consumer);
            }
        }
    }
}
