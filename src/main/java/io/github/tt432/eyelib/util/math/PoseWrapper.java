package io.github.tt432.eyelib.util.math;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AllArgsConstructor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * @author TT432
 */
@AllArgsConstructor
public class PoseWrapper implements AutoCloseable {
    PoseStack.Pose source;
    Matrix4f pose;
    Matrix3f normal;

    public Matrix4f pose() {
        return pose;
    }

    public Matrix3f normal() {
        return normal;
    }

    public static PoseWrapper from(PoseStack.Pose pose) {
        return new PoseWrapper(pose, Jomls.from(pose.pose()).transpose(), Jomls.from(pose.normal()).transpose());
    }

    public void to(PoseStack.Pose pose) {
        com.mojang.math.Matrix4f sourcePose = pose.pose();
        com.mojang.math.Matrix3f sourceNormal = pose.normal();
        this.pose.transpose();
        this.normal.transpose();
        sourcePose.m00 = this.pose.m00();
        sourcePose.m01 = this.pose.m01();
        sourcePose.m02 = this.pose.m02();
        sourcePose.m03 = this.pose.m03();
        sourcePose.m10 = this.pose.m10();
        sourcePose.m11 = this.pose.m11();
        sourcePose.m12 = this.pose.m12();
        sourcePose.m13 = this.pose.m13();
        sourcePose.m20 = this.pose.m20();
        sourcePose.m21 = this.pose.m21();
        sourcePose.m22 = this.pose.m22();
        sourcePose.m23 = this.pose.m23();
        sourcePose.m30 = this.pose.m30();
        sourcePose.m31 = this.pose.m31();
        sourcePose.m32 = this.pose.m32();
        sourcePose.m33 = this.pose.m33();

        sourceNormal.m00 = normal.m00;
        sourceNormal.m01 = normal.m01;
        sourceNormal.m02 = normal.m02;
        sourceNormal.m10 = normal.m10;
        sourceNormal.m11 = normal.m11;
        sourceNormal.m12 = normal.m12;
        sourceNormal.m20 = normal.m20;
        sourceNormal.m21 = normal.m21;
        sourceNormal.m22 = normal.m22;
    }

    @Override
    public void close() {
        to(source);
    }
}
