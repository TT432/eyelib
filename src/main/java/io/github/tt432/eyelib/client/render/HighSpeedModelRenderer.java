package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.visitor.ModelRenderVisitorList;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

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
            for (List<Vector3fc> vertexList : cube.vertexes()) {
                vertexes.addAll(vertexList);

                for (int i1 = vertexList.size() - 1; i1 >= 0; i1--) {
                    vertexes.add(vertexList.get(i1));
                }
            }

            for (var uvList : cube.uvs()) {
                uvs.addAll(uvList);

                for (int i1 = uvList.size() - 1; i1 >= 0; i1--) {
                    uvs.add(uvList.get(i1));
                }
            }

            for (int i = 0; i < cube.normals().size(); i++) {
                for (int j = 0; j < 8; j++) {
                    normals.add(cube.normals().get(i));
                }
            }
        }
    }

    private static final float R180 = 180 * EyeMath.DEGREES_TO_RADIANS;

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static <R extends ModelRuntimeData<?, ?, R>> void render(RenderParams renderParams, Model model, R infos,
                                                                    @Nullable BrModelTextures.TwoSideInfoMap map,
                                                                    ModelRenderVisitorList visitors, HBakedModel bakedModel) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.rotateY(R180);
        Matrix3f normal = last.normal();
        normal.rotateY(R180);

        visitors.visitors().forEach(v -> v.visitModel(renderParams));

        for (var toplevelBone : model.toplevelBones().values()) {
            renderBone(renderParams, map, visitors, cast(infos), toplevelBone, model.locator().getGroup(toplevelBone.name()), bakedModel);
        }

        poseStack.popPose();
    }

    @SuppressWarnings("unchecked")
    private static <G extends Model.Bone, R extends ModelRuntimeData<G, Object, R>> void renderBone(
            RenderParams renderParams, @Nullable BrModelTextures.TwoSideInfoMap map,
            ModelRenderVisitorList visitors, R infos, G bone, GroupLocator locatorsGroup, HBakedModel bakedModel
    ) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        ModelTransformer<G, R> transformer = infos.transformer();

        applyBoneTranslate(poseStack, bone, infos, transformer);

        visitors.visitors().forEach(visitor -> visitor.visitBone(renderParams, bone, infos, transformer));

        visitLocators(renderParams, visitors, bone, poseStack, bone, infos, transformer, locatorsGroup);

        VertexConsumer consumer = renderParams.consumer();
        int overlay = renderParams.overlay();
        int light = renderParams.light();

        var bakedBone = bakedModel.bones.get(bone.name());

        PoseStack.Pose last = poseStack.last();
        bakedBone.transformPos(last.pose());
        bakedBone.transformNormal(last.normal());

        visitVertex(bakedBone, consumer, overlay, light);

        for (var child : bone.children().values()) {
            renderBone(renderParams, map, visitors, infos, (G) child, locatorsGroup.getChild(child.name()), bakedModel);
        }

        poseStack.popPose();
    }

    private static void visitVertex(HBakedBone bakedBone, VertexConsumer consumer, int overlay, int light) {
        for (int nIdx = 0; nIdx < bakedBone.nxList.length; nIdx++) {
            consumer.addVertex(bakedBone.xListResult[nIdx], bakedBone.yListResult[nIdx], bakedBone.zListResult[nIdx],
                    0xFF_FF_FF_FF, bakedBone.u[nIdx], bakedBone.v[nIdx], overlay, light,
                    bakedBone.nxListResult[nIdx], bakedBone.nyListResult[nIdx], bakedBone.nzListResult[nIdx]);
        }
    }

    private static <G extends Model.Bone, R extends ModelRuntimeData<G, Object, R>> void visitLocators(
            RenderParams renderParams, ModelRenderVisitorList visitors, Model.Bone bone, PoseStack poseStack,
            G group, R data, ModelTransformer<G, R> transformer, GroupLocator locatorsGroup
    ) {
        if (locatorsGroup == null) return;
        List<LocatorEntry> cubes = locatorsGroup.cubes();
        if (cubes == null) return;

        cubes.forEach(locator -> {
            poseStack.pushPose();

            PoseStack.Pose last1 = poseStack.last();
            Matrix4f pose = last1.pose();
            pose.translate(locator.offset());
            pose.rotateZYX(locator.rotation());
            last1.normal().rotateZYX(locator.rotation());

            visitors.visitors().forEach(visitor -> visitor.visitLocator(renderParams, bone, locator.name(), locator, group, data, transformer));

            poseStack.popPose();
        });
    }

    private static <G extends Model.Bone, R extends ModelRuntimeData<G, Object, R>> void applyBoneTranslate(
            PoseStack poseStack, G model, R data, ModelTransformer<G, R> transformer
    ) {
        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();

        pose.translate(transformer.position(model, data));

        var renderPivot = transformer.pivot(model, data);
        pose.translate(renderPivot);

        var rotation = transformer.rotation(model, data);

        last.normal().rotateZYX(rotation.z(), rotation.y(), rotation.x());
        last.pose().rotateZYX(rotation.z(), rotation.y(), rotation.x());

        var scale = transformer.scale(model, data);
        poseStack.scale(scale.x(), scale.y(), scale.z());

        pose.translate(-renderPivot.x(), -renderPivot.y(), -renderPivot.z());
    }
}
