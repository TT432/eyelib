package io.github.tt432.eyelib.client.render;

import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * 一次模型渲染的产物（G2）：调用方不再需要靠装饰 {@code RenderSink} 抄「首个 submit 的矩阵」。
 * <p>
 * 矩阵空间约定：{@code entityPose} 是宿主位姿（{@code action.poseStack()} 顶部的副本，未施加
 * 脚本缩放/幼体缩放/睡眠位移/身体朝向）；{@code rootPose} 是模型根位姿，即宿主位姿经
 * {@link RenderHost#applyRootPose} 之后的矩阵。两者都**不含**模型 Y 180° 翻转——
 * {@link io.github.tt432.eyelib.animation.ModelPoseTransforms} 的入口自己施加该翻转，
 * 因此 {@code rootPose} 可直接作为它们的 {@code rootPose} 实参，也与内置挂载物使用的
 * {@code modelRoot} 同值。
 * <p>
 * 两个矩阵都是新副本，调用方可以跨帧长期持有（26.1 的延迟 writer 执行时原 {@code PoseStack}
 * 早已弹出，因此不能返回引用）。
 *
 * @param entityPose     宿主位姿；未拿到位姿（无宿主/无 scope）时为 null
 * @param rootPose       模型根位姿；无几何提交的跳过路径上可能为 null
 * @param submittedPasses 本帧经 sink 提交的模型几何段数
 * @param drawnComponents 参与了本帧绘制的模型组件数
 * @param attachmentDraws 挂载阶段实际绘制的段数
 * @param skipReason     跳过原因；{@code null} 表示本帧确实提交了几何
 * @author TT432
 */
public record RenderResult(
        @Nullable Matrix4f entityPose,
        @Nullable Matrix4f rootPose,
        int submittedPasses,
        int drawnComponents,
        int attachmentDraws,
        @Nullable RenderSkipReason skipReason
) {

    public boolean drawn() {
        return skipReason == null;
    }

    /** 无位姿可言的跳过（缺宿主 / scope 未初始化 / 缺动画）。 */
    public static RenderResult skipped(RenderSkipReason reason) {
        return new RenderResult(null, null, 0, 0, 0, reason);
    }

    /** 已拿到宿主位姿但未提交几何的跳过（无组件 / 未就绪 / 未烘焙）。 */
    public static RenderResult skipped(RenderSkipReason reason, @Nullable Matrix4f entityPose, @Nullable Matrix4f rootPose) {
        return new RenderResult(entityPose, rootPose, 0, 0, 0, reason);
    }

    /** 提交过几何的成功结果。 */
    public static RenderResult drawn(@Nullable Matrix4f entityPose, @Nullable Matrix4f rootPose,
                                     int submittedPasses, int drawnComponents, int attachmentDraws) {
        return new RenderResult(entityPose, rootPose, submittedPasses, drawnComponents, attachmentDraws, null);
    }
}
