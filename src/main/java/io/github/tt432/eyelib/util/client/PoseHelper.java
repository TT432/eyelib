package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.util.client.render.PoseCopies;

public class PoseHelper {
    public static PoseStack.Pose copy(PoseStack.Pose pose) {
        return PoseCopies.copy(pose);
    }
}
