package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.client.render.BrModelRenderVisitor;
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

    public static void render(BrModel model, PoseStack poseStack, VertexConsumer consumer, BrModelRenderVisitor visitor) {
        for (BrBone toplevelBone : model.toplevelBones()) {
            renderBone(poseStack, visitor, toplevelBone, consumer);
        }
    }

    private static void renderBone(PoseStack poseStack, BrModelRenderVisitor visitor, BrBone bone, VertexConsumer consumer) {
        poseStack.pushPose();

        visitor.visitBone(poseStack, bone, consumer, true);

        Matrix4f pose = poseStack.last().pose();
        Vector3f renderPivot = bone.getRenderPivot();

        if (renderPivot == null) {
            renderPivot = bone.getPivot();
        }

        pose.translate(renderPivot);

        Vector3f rotation = bone.getRenderRotation();

        if (rotation == null) {
            rotation = bone.getRotation();
        }

        poseStack.mulPose(tQ.rotationZYX(rotation.z, rotation.y, rotation.x));
        pose.translate(renderPivot.negate(nPivot));

        Vector3f scale = bone.getRenderScala();
        poseStack.scale(scale.x, scale.y, scale.z);

        visitor.visitBone(poseStack, bone, consumer, false);

        for (BrCube cube : bone.getCubes()) {
            renderCube(poseStack, visitor, cube, consumer);
        }

        for (BrBone child : bone.getChildren()) {
            renderBone(poseStack, visitor, child, consumer);
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
