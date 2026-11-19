package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 挂载内容的绘制实现（G3）。
 * <p>
 * 调用时机固定在**模型几何提交之前、模型根位姿作用域内**：26.1 的 submit 节点在几何 writer 内
 * 已不再接收新内容，因此挂载必须先于本体提交。传入的 {@link PoseStack} 已经定位到锚点位姿，
 * 实现只需在其上继续施加局部变换并绘制。
 *
 * @author TT432
 */
@FunctionalInterface
public interface AttachmentDraw {
    /**
     * @param frame      本帧上下文（位姿/采样/sink/光照）
     * @param pose       已定位到锚点的位姿栈（每个锚点实例独立一份，可自由 push/pop）
     * @param anchorName 当前锚点实例的名称（骨骼名 / locator 名 / 空串）
     * @return 本次提交的几何段数；0 表示未绘制（观察型声明，例如只记录乘客挂点）
     */
    int draw(RenderFrame frame, PoseStack pose, String anchorName);
}
