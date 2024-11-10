package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.util.math.EyeMath;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
public class ModelVisitor {
    private static final float R180 = 180 * EyeMath.DEGREES_TO_RADIANS;

    @SuppressWarnings("unchecked")
    protected static <T> T cast(Object o) {
        return (T) o;
    }

    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitModel(RenderParams params, Context context, D infos, Model model) {
        PoseStack poseStack = params.poseStack();
        poseStack.pushPose();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.rotateY(R180);
        Matrix3f normal = last.normal();
        normal.rotateY(R180);

        for (var toplevelBone : model.toplevelBones().values()) {
            visitBone(params, context, model, toplevelBone, cast(infos), model.locator().getGroup(toplevelBone.name()), infos.transformer());
        }

        poseStack.popPose();
    }

    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitBone(RenderParams renderParams, Context context,
                                                                         Model model, Model.Bone group, D data, GroupLocator groupLocator,
                                                                         ModelTransformer<Model.Bone, D> transformer) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(poseStack, group, cast(data), transformer);

        visitLocators(renderParams, context, group, poseStack, data, transformer, groupLocator);

        for (int i = 0; i < group.cubes().size(); i++) {
            visitCube(renderParams, context, group.cubes().get(i));
        }

        for (var child : group.children().values()) {
            visitBone(renderParams, context, model, child, data, groupLocator.getChild(child.name()), transformer);
        }

        poseStack.popPose();
    }

    public void visitCube(RenderParams renderParams, Context context, Model.Cube cube) {
        for (int i = 0; i < cube.vertexes().size(); i++) {
            List<Vector3fc> vertexes = cube.vertexes().get(i);
            List<Vector2fc> uvs = cube.uvs().get(i);
            Vector3fc normal = cube.normals().get(i);

            visitFace(renderParams, context, cube, vertexes, uvs, normal);

            for (int vertexIndex = 0; vertexIndex < vertexes.size(); vertexIndex++) {
                Vector3fc vertex = vertexes.get(vertexIndex);
                Vector2fc uv = uvs.get(vertexIndex);

                visitVertex(renderParams, context, cube, vertex, uv, normal);
            }

            for (int vertexIndex = vertexes.size() - 1; vertexIndex >= 0; vertexIndex--) {
                Vector3fc vertex = vertexes.get(vertexIndex);
                Vector2fc uv = uvs.get(vertexIndex);

                visitVertex(renderParams, context, cube, vertex, uv, normal);
            }
        }
    }

    public void visitFace(RenderParams renderParams, Context context, Model.Cube cube,
                          List<Vector3fc> vertexes, List<Vector2fc> uvs, Vector3fc normal) {

    }

    public void visitVertex(RenderParams renderParams, Context context, Model.Cube cube,
                            Vector3fc vertex, Vector2fc uv, Vector3fc normal) {

    }

    public <G extends Model.Bone, R extends ModelRuntimeData<G, ?, R>> void visitLocator(
            RenderParams renderParams, Context context, Model.Bone bone,
            LocatorEntry locator, R data, ModelTransformer<G, R> transformer
    ) {

    }

    protected static <R extends ModelRuntimeData<Model.Bone, ?, R>> void applyBoneTranslate(
            PoseStack poseStack, Model.Bone model, R data, ModelTransformer<Model.Bone, R> transformer
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

    protected <R extends ModelRuntimeData<Model.Bone, ?, R>> void visitLocators(
            RenderParams renderParams, Context context, Model.Bone bone, PoseStack poseStack,
            R data, ModelTransformer<Model.Bone, R> transformer, GroupLocator locatorsGroup
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

            visitLocator(renderParams, context, bone, locator, data, transformer);

            poseStack.popPose();
        });
    }

    public static final class Context {
        private final Map<String, Object> data = new HashMap<>();

        public void put(String key, Object value) {
            data.put(key, value);
        }

        public boolean contains(String key) {
            return data.containsKey(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            return (T) data.get(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T orCreate(String key, T value) {
            return (T) data.computeIfAbsent(key, s -> value);
        }

        public void clear() {
            data.clear();
        }
    }
}
