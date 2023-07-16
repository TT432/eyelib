package io.github.tt432.eyelib.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrFace;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
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
    /**
     * visitor:
     * <pre>{@code
     * new BrModelRenderVisitor(){
     *     @Override
     *     public void visitVertex(PoseStack poseStack, BrCube cube,
     *                             BrFace face, int vertexId, VertexConsumer consumer) {
     *         Vector3f normal = face.getNormal();
     *         Vector3f vertex = face.getVertex()[vertexId];
     *         Vector2f uv = face.getUv()[vertexId];
     *
     *         consumer.vertex(poseStack.last().pose(), vertex.x, vertex.y, vertex.z)
     *                 .color(0xFF_FF_FF_FF)
     *                 .uv(uv.x, uv.y)
     *                 .overlayCoords(OverlayTexture.NO_OVERLAY)
     *                 .uv2(LightTexture.FULL_BRIGHT)
     *                 .normal(poseStack.last().normal(), normal.x, normal.y, normal.z)
     *                 .endVertex();
     *     }
     * });
     * }</pre>
     *
     * @param poseStack p
     * @param model     m
     * @param consumer  c
     * @param visitor   v
     */
    public static void fillBrModelVertex(PoseStack poseStack, BrModel model, VertexConsumer consumer, BrModelRenderVisitor visitor) {
        visitor.visitModel(poseStack, model, consumer);

        model.getToplevelBones().forEach(bone -> fillBrBoneVertex(poseStack, bone, consumer, visitor));
    }

    public static void fillBrBoneVertex(PoseStack poseStack, BrBone bone, VertexConsumer consumer, BrModelRenderVisitor visitor) {
        poseStack.pushPose();

        Matrix4f pose = poseStack.last().pose();
        pose.translate(bone.getPivot());

        Vector3f rotation = bone.getRotation();
        poseStack.mulPose(new Quaternionf().rotationXYZ(rotation.x, rotation.y, rotation.z));

        pose.translate(bone.getPivot().negate(new Vector3f()));

        visitor.visitBone(poseStack, bone, consumer);

        bone.getCubes().forEach(cube -> fillBrCubeVertex(poseStack, cube, consumer, visitor));

        bone.getChildren().forEach(child -> fillBrBoneVertex(poseStack, child, consumer, visitor));

        poseStack.popPose();
    }

    public static void fillBrCubeVertex(PoseStack poseStack, BrCube cube, VertexConsumer consumer, BrModelRenderVisitor visitor) {
        visitor.visitCube(poseStack, cube, consumer);

        for (BrFace face : cube.getFaces()) {
            for (int i = 0; i < face.getVertex().length; i++) {
                visitor.visitVertex(poseStack, cube, face, i, consumer);
            }

            for (int i = face.getVertex().length - 1; i >= 0; i--) {
                visitor.visitVertex(poseStack, cube, face, i, consumer);
            }
        }
    }
}
