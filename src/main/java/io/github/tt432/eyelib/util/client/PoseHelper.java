package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.vertex.PoseStack;

public class PoseHelper {
    public static PoseStack.Pose copy(PoseStack.Pose pose) {
        return new PoseStack.Pose(pose.pose(), pose.normal());
    }
}
