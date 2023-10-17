package io.github.tt432.eyelib.client.model.flat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.render.BrModelRenderVisitor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public class EnterBoneCommand implements FlatBrModelCommand{
    final BrBone bone;

    public EnterBoneCommand(BrBone bone) {
        this.bone = bone;
    }

    @Override
    public void doCommand(PoseStack poseStack, VertexConsumer consumer, BrModelRenderVisitor visitor) {
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

        poseStack.mulPose(new Quaternionf().rotationZYX(rotation.z, rotation.y, rotation.x));
        pose.translate(renderPivot.negate(new Vector3f()));

        Vector3f scale = bone.getRenderScala();
        poseStack.scale(scale.x, scale.y, scale.z);

        visitor.visitBone(poseStack, bone, consumer, false);
    }
}
