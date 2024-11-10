package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.visitor.BuiltInBrModelRenderVisitors;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import lombok.experimental.UtilityClass;
import org.joml.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
@UtilityClass
public class HighSpeedModelRenderer {
    public record HBakedModel(Map<String, HBakedBone> bones) {
        public static HBakedModel bake(Model model) {
            Map<String, HBakedBone> bones = new HashMap<>();

            model.toplevelBones().forEach((s, bone) -> collectBones(s, bone, bones));

            return new HBakedModel(bones);
        }

        private static void collectBones(String name, Model.Bone bone, Map<String, HBakedBone> bones) {
            bones.put(name, HBakedBone.bake(bone));
            bone.children().forEach((s, bone1) -> collectBones(s, bone1, bones));
        }
    }

    public record HBakedBone(
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

        public static HBakedBone bake(Model.Bone bone) {
            List<Vector3fc> vertexes = new ArrayList<>();
            List<Vector3fc> normals = new ArrayList<>();
            List<Vector2fc> uvs = new ArrayList<>();

            for (Model.Cube cube : bone.cubes()) {
                bake(cube, vertexes, normals, uvs);
            }

            float[] xList = new float[vertexes.size()];
            float[] yList = new float[vertexes.size()];
            float[] zList = new float[vertexes.size()];

            float[] nxList = new float[normals.size()];
            float[] nyList = new float[normals.size()];
            float[] nzList = new float[normals.size()];

            float[] u = new float[uvs.size()];
            float[] v = new float[uvs.size()];

            for (int i = 0; i < vertexes.size(); i++) {
                xList[i] = vertexes.get(i).x();
                yList[i] = vertexes.get(i).y();
                zList[i] = vertexes.get(i).z();
            }

            for (int i = 0; i < normals.size(); i++) {
                nxList[i] = normals.get(i).x();
                nyList[i] = normals.get(i).y();
                nzList[i] = normals.get(i).z();
            }

            for (int i = 0; i < uvs.size(); i++) {
                u[i] = uvs.get(i).x();
                v[i] = uvs.get(i).y();
            }

            return new HBakedBone(
                    xList, yList, zList, nxList, nyList, nzList,
                    new float[vertexes.size() * 3], new float[vertexes.size() * 3], new float[vertexes.size() * 3],
                    new float[normals.size() * 3], new float[normals.size() * 3], new float[normals.size() * 3],
                    u, v
            );
        }

        private static void bake(Model.Cube cube, List<Vector3fc> vertexes, List<Vector3fc> normals, List<Vector2fc> uvs) {
            for (int i = 0; i < cube.faceCount(); i++) {
                for (int j = 0; j < cube.pointsPerFace(); j++) {
                    vertexes.add(new Vector3f(cube.positionX(i, j), cube.positionY(i, j), cube.positionZ(i, j)));

                    uvs.add(new Vector2f(cube.uvU(i, j), cube.uvV(i, j)));
                }

                for (int j = cube.pointsPerFace() - 1; j >= 0; j--) {
                    vertexes.add(new Vector3f(cube.positionX(i, j), cube.positionY(i, j), cube.positionZ(i, j)));

                    uvs.add(new Vector2f(cube.uvU(i, j), cube.uvV(i, j)));
                }

                for (int j = 0; j < 8; j++) {
                    normals.add(new Vector3f(cube.normalX(i), cube.normalY(i), cube.normalZ(i)));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static <R extends ModelRuntimeData<?, ?, R>> void render(RenderParams renderParams, Model model, R infos,
                                                                    ModelRenderVisitorList visitors, HBakedModel bakedModel) {
        ModelVisitor.Context context = new ModelVisitor.Context();
        context.put("HBackedModel", bakedModel);
        BuiltInBrModelRenderVisitors.HIGH_SPEED_RENDER.get().visitModel(renderParams, context, cast(infos), model);
    }
}
