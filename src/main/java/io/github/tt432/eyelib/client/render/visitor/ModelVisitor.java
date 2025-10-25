package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.joml.*;

import java.util.Deque;
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

    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitModel(RenderParams params, ModelVisitContext context, D infos, Model model) {
        model.accept(params, context, infos, this);
    }

    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPreModel(RenderParams params, ModelVisitContext context, D infos, Model model) {
        PoseStack poseStack = params.poseStack();
        poseStack.pushPose();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.rotateY(R180);
        Matrix3f normal = last.normal();
        normal.rotateY(R180);
    }

    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPostModel(RenderParams params, ModelVisitContext context, D infos, Model model) {
        PoseStack poseStack = params.poseStack();
        poseStack.popPose();
    }

    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPreBone(RenderParams renderParams, ModelVisitContext context,
                                                                            Model.Bone group, D data, GroupLocator groupLocator,
                                                                            ModelTransformer<Model.Bone, D> transformer) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(context, poseStack, group, cast(data), transformer);
    }

    public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPostBone(RenderParams renderParams, ModelVisitContext context,
                                                                             Model.Bone group, D data, GroupLocator groupLocator,
                                                                             ModelTransformer<Model.Bone, D> transformer) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.popPose();
    }

    public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
        for (int i = 0; i < cube.faceCount(); i++) {
            List<Vector3f> vertexes = cube.vertexes().get(i);
            List<Vector2f> uvs = cube.uvs().get(i);
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

    public void visitFace(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                          List<Vector3f> vertexes, List<Vector2f> uvs, Vector3fc normal) {

    }

    public void visitVertex(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                            Vector3fc vertex, Vector2fc uv, Vector3fc normal) {

    }

    public <R extends ModelRuntimeData<Model.Bone, ?, R>> void visitLocator(
            RenderParams renderParams, ModelVisitContext context, Model.Bone bone,
            LocatorEntry locator, R data, ModelTransformer<Model.Bone, R> transformer
    ) {

    }

    protected static <R extends ModelRuntimeData<Model.Bone, ?, R>> void applyBoneTranslate(
            ModelVisitContext context, PoseStack poseStack, Model.Bone model, R data, ModelTransformer<Model.Bone, R> transformer
    ) {
        context.<Map<String, PoseStack.Pose>>orCreate("bones", new Object2ObjectOpenHashMap<>()).compute(model.name(), (n, pose) -> {
            if (pose == null) {
                PoseStack.Pose last = poseStack.last();
                Matrix4f m4 = last.pose();

                m4.translate(transformer.position(model, data));

                var renderPivot = transformer.pivot(model, data);
                m4.translate(renderPivot);

                var rotation = transformer.rotation(model, data);

                last.normal().rotateZYX(rotation.z(), rotation.y(), rotation.x());
                m4.rotateZYX(rotation.z(), rotation.y(), rotation.x());

                var scale = transformer.scale(model, data);
                poseStack.scale(scale.x(), scale.y(), scale.z());

                m4.translate(-renderPivot.x(), -renderPivot.y(), -renderPivot.z());
                return last.copy();
            } else {
                Deque<PoseStack.Pose> stack = poseStack.poseStack;
                stack.removeLast();
                stack.addLast(pose);
                return pose;
            }
        });
    }
}
