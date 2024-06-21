package io.github.tt432.eyelib.util.math;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public class Jomls {
    public static Vector3f from(com.mojang.math.Vector3f source) {
        return new Vector3f(source.x(), source.y(), source.z());
    }

    public static Matrix4f from(com.mojang.math.Matrix4f source) {
        return new Matrix4f(source.m00, source.m01, source.m02, source.m03,
                source.m10, source.m11, source.m12, source.m13,
                source.m20, source.m21, source.m22, source.m23,
                source.m30, source.m31, source.m32, source.m33);
    }

    public static Matrix3f from(com.mojang.math.Matrix3f source) {
        return new Matrix3f(
                source.m00, source.m01, source.m02,
                source.m10, source.m11, source.m12,
                source.m20, source.m21, source.m22
        );
    }
}
