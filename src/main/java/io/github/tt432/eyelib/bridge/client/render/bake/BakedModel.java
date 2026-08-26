package io.github.tt432.eyelib.bridge.client.render.bake;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
/**
 * 预烘焙的模型数据。
 *
 * @author TT432
 */
public record BakedModel(
        Int2ObjectMap<BakedBone> bones
) {
    public record BakedBone(
            int vertexSize,

            float[] position,
            float[] normal,

            float[] positionResult,
            float[] normalResult,

            float[] u,
            float[] v
    ) {
        private static float[] merge(float[] x, float[] y, float[] z) {
            float[] result = new float[x.length * 3];

            for (int i = 0; i < x.length; i++) {
                result[i * 3] = x[i];
                result[i * 3 + 1] = y[i];
                result[i * 3 + 2] = z[i];
            }

            return result;
        }

        public BakedBone(float[] xList, float[] yList, float[] zList,
                         float[] nxList, float[] nyList, float[] nzList,
                         float[] xListResult, float[] yListResult, float[] zListResult,
                         float[] nxListResult, float[] nyListResult, float[] nzListResult,
                         float[] u, float[] v) {
            this(xList.length, merge(xList, yList, zList), merge(nxList, nyList, nzList),
                 merge(xListResult, yListResult, zListResult), merge(nxListResult, nyListResult, nzListResult),
                 u, v);
        }

        public void transformPos(Matrix4f m4) {
            float m00 = m4.m00(), m01 = m4.m01(), m02 = m4.m02();
            float m10 = m4.m10(), m11 = m4.m11(), m12 = m4.m12();
            float m20 = m4.m20(), m21 = m4.m21(), m22 = m4.m22();
            float m30 = m4.m30(), m31 = m4.m31(), m32 = m4.m32();

            for (int i = 0; i < vertexSize; i++) {
                float x = position[i * 3];
                float y = position[i * 3 + 1];
                float z = position[i * 3 + 2];

                positionResult[i * 3] = m00 * x + (m10 * y + (m20 * z + m30));
                positionResult[i * 3 + 1] = m01 * x + (m11 * y + (m21 * z + m31));
                positionResult[i * 3 + 2] = m02 * x + (m12 * y + (m22 * z + m32));
            }
        }

        public void transformNormal(Matrix3f m3) {
            float m00 = m3.m00(), m01 = m3.m01(), m02 = m3.m02();
            float m10 = m3.m10(), m11 = m3.m11(), m12 = m3.m12();
            float m20 = m3.m20(), m21 = m3.m21(), m22 = m3.m22();

            for (int i = 0; i < vertexSize; i++) {
                var nx = normal[i * 3];
                var ny = normal[i * 3 + 1];
                var nz = normal[i * 3 + 2];

                float rx = m00 * nx + (m10 * ny + (m20 * nz));
                float ry = m01 * nx + (m11 * ny + (m21 * nz));
                float rz = m02 * nx + (m12 * ny + (m22 * nz));

                // light.glsl 不 normalize normal，需在此保证单位长度，对齐原版 PoseStack.Pose.transformNormal。
                float lenSq = rx * rx + ry * ry + rz * rz;
                if (lenSq > 1.0E-8F) {
                    float inv = 1.0F / (float) Math.sqrt(lenSq);
                    normalResult[i * 3] = rx * inv;
                    normalResult[i * 3 + 1] = ry * inv;
                    normalResult[i * 3 + 2] = rz * inv;
                } else {
                    normalResult[i * 3] = rx;
                    normalResult[i * 3 + 1] = ry;
                    normalResult[i * 3 + 2] = rz;
                }
            }
        }
    }
}
