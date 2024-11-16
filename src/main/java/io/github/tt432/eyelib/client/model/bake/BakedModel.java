package io.github.tt432.eyelib.client.model.bake;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;

/**
 * @author TT432
 */
public record BakedModel(
        Map<String, BakedBone> bones
) {
    public record BakedBone(
            float[] xList,
            float[] yList,
            float[] zList,
            float[] nxList,
            float[] nyList,
            float[] nzList,

            float[] xListResult,
            float[] yListResult,
            float[] zListResult,
            float[] nxListResult,
            float[] nyListResult,
            float[] nzListResult,

            float[] u,
            float[] v
    ) {
        public void transformPos(Matrix4f m4) {
            float m00 = m4.m00(), m01 = m4.m01(), m02 = m4.m02();
            float m10 = m4.m10(), m11 = m4.m11(), m12 = m4.m12();
            float m20 = m4.m20(), m21 = m4.m21(), m22 = m4.m22();
            float m30 = m4.m30(), m31 = m4.m31(), m32 = m4.m32();

            int length = xList.length;
            for (int i = 0; i < length; i++) {
                float x = xList[i];
                float y = yList[i];
                float z = zList[i];

                xListResult[i] = m00 * x + (m10 * y + (m20 * z + m30));
                yListResult[i] = m01 * x + (m11 * y + (m21 * z + m31));
                zListResult[i] = m02 * x + (m12 * y + (m22 * z + m32));
            }
        }

        public void transformNormal(Matrix3f m3) {
            float m00 = m3.m00(), m01 = m3.m01(), m02 = m3.m02();
            float m10 = m3.m10(), m11 = m3.m11(), m12 = m3.m12();
            float m20 = m3.m20(), m21 = m3.m21(), m22 = m3.m22();

            int length = nxList.length;
            for (int i = 0; i < length; i++) {
                var nx = nxList[i];
                var ny = nyList[i];
                var nz = nzList[i];

                nxListResult[i] = m00 * nx + (m10 * ny + (m20 * nz));
                nyListResult[i] = m01 * nx + (m11 * ny + (m21 * nz));
                nzListResult[i] = m02 * nx + (m12 * ny + (m22 * nz));
            }
        }
    }
}