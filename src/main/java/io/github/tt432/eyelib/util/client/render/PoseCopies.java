package io.github.tt432.eyelib.util.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PoseCopies {
    public static PoseStack.Pose copy(PoseStack.Pose pose) {
        return new PoseStack.Pose(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()));
    }
}
