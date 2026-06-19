//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.21.1 中 {@code PoseStack.Pose} 的构造器变 private，
 * 用 {@code @Invoker("<init>")} 暴露。
 *
 * @author TT432
 */
@Mixin(PoseStack.Pose.class)
public interface PoseStackPoseAccessor {
    @Invoker("<init>")
    static PoseStack.Pose eyelib$create(Matrix4f matrix4f, Matrix3f matrix3f) {
        throw new AssertionError();
    }
}
//?}
