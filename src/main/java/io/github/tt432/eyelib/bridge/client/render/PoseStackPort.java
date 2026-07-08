package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Deque;

/**
 * PoseStack / PoseStack.Pose 版本差异 Port，屏蔽不同版本间构造、复制和替换的 API 差异。
 *
 * @author TT432
 */
public interface PoseStackPort {

    static PoseStack.Pose copy(PoseStack.Pose pose) {
        //? if <1.20.6 {
        return new PoseStack.Pose(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()));
        //?} else {
        return pose.copy();
        //?}
    }

    static PoseStack.Pose identity() {
        //? if <1.20.6 {
        return new PoseStack.Pose(new Matrix4f(), new Matrix3f());
        //?} elif <26.1 {
        return io.github.tt432.eyelib.mixin.PoseStackPoseAccessor.eyelib$create(new Matrix4f(), new Matrix3f());
        //?} else {
        return new PoseStack().last();
        //?}
    }

    static void replaceLast(PoseStack poseStack, PoseStack.Pose pose) {
        //? if <26.1 {
        Deque<PoseStack.Pose> stack;
        //? if <1.20.6 {
        stack = poseStack.poseStack;
        //?} else {
        stack = ((io.github.tt432.eyelib.mixin.PoseStackAccessor) poseStack).eyelib$getPoseStackDeque();
        //?}
        stack.removeLast();
        stack.addLast(pose);
        //?} else {
        poseStack.last().set(pose);
        //?}
    }

    static Matrix4f getLastPoseMatrix(PoseStack poseStack) {
        //? if <1.20.6 {
        return new Matrix4f(poseStack.poseStack.getLast().pose());
        //?} elif <26.1 {
        return new Matrix4f(((io.github.tt432.eyelib.mixin.PoseStackAccessor) poseStack).eyelib$getPoseStackDeque().getLast().pose());
        //?} else {
        return new Matrix4f(poseStack.last().pose());
        //?}
    }
}
