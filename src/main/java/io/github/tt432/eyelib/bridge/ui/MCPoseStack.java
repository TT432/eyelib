package io.github.tt432.eyelib.bridge.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.ui.UIPoseStack;

/**
 * 将 MC {@link PoseStack} 适配为 {@link UIPoseStack}。
 *
 * @author TT432
 */
public final class MCPoseStack implements UIPoseStack {
    private final PoseStack pose;

    public MCPoseStack(PoseStack pose) {
        this.pose = pose;
    }

    @Override
    public void pushPose() {
        pose.pushPose();
    }

    @Override
    public void popPose() {
        pose.popPose();
    }

    @Override
    public void translate(double x, double y, double z) {
        pose.translate(x, y, z);
    }

    @Override
    public void scale(float x, float y, float z) {
        pose.scale(x, y, z);
    }

    @Override
    public void rotateX(float degrees) {
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(degrees));
    }

    @Override
    public void rotateY(float degrees) {
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(degrees));
    }
}
