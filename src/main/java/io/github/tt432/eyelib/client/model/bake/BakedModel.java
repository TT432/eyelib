package io.github.tt432.eyelib.client.model.bake;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY;

/**
 * @author TT432
 */
public record BakedModel(
        Map<String, BakedBone> bones
) {
    public record BakedBone(
            int vertexSize,

            float[] position,
            float[] normal,

            float[] positionResult,
            float[] normalResult,

            float[] u,
            float[] v,
            BufferBuilder vertices
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
                    u, v, new BufferBuilder(new ByteBufferBuilder(NEW_ENTITY.getVertexSize() * xList.length), VertexFormat.Mode.QUADS, NEW_ENTITY));
            for (int i = 0; i < xList.length; i++) {
                vertices.addVertex(xList[i], yList[i], zList[i],
                        0, u[i], v[i], 0, 0,
                        nxList[i], nyList[i], nzList[i]);
            }
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

                normalResult[i * 3] = m00 * nx + (m10 * ny + (m20 * nz));
                normalResult[i * 3 + 1] = m01 * nx + (m11 * ny + (m21 * nz));
                normalResult[i * 3 + 2] = m02 * nx + (m12 * ny + (m22 * nz));
            }
        }
    }
}