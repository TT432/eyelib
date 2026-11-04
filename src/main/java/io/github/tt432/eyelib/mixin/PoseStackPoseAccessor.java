//? if >=1.20.6 && <26.1 {
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
 * <p>26.1 起 {@code PoseStack.Pose} 改为可变字段（{@code pose().set(...)}），构造器不再存在，
 * 该 accessor 必须从 26.1 构建中排除（此前只在 {@code @Invoker} 定位失败时打 WARN，
 * 属生产日志噪声：eyelib.mixins.json 曾对所有非 legacy 版本登记它）。
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
