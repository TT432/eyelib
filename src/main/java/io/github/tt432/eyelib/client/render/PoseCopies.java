package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

/** @author TT432 */
@NullMarked
public final class PoseCopies {
    public static PoseStack.Pose copy(PoseStack.Pose pose) {
        return new PoseStack.Pose(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()));
    }
}
