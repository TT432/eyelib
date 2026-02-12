package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.*;

import java.util.Deque;
import java.util.List;

/**
 * @author TT432
 */
public class ModelVisitor {
    private static final float R180 = 180 * EyeMath.DEGREES_TO_RADIANS;

    @SuppressWarnings("unchecked")
    protected static <T> T cast(Object o) {
        return (T) o;
    }

    public <B extends Model.Bone<B>> void visitModel(RenderParams params, ModelVisitContext context, ModelRuntimeData<B> infos, Model<B> model) {
        model.accept(params, context, infos, this);
    }

    public <B extends Model.Bone<B>> void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData<B> infos, Model<B> model) {
        PoseStack poseStack = params.poseStack();
        poseStack.pushPose();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.rotateY(R180);
        Matrix3f normal = last.normal();
        normal.rotateY(R180);
    }

    public <B extends Model.Bone<B>> void visitPostModel(RenderParams params, ModelVisitContext context, ModelRuntimeData<B> infos, Model<B> model) {
        PoseStack poseStack = params.poseStack();
        poseStack.popPose();
    }

    public <B extends Model.Bone<B>> void visitPreBone(RenderParams renderParams, ModelVisitContext context,
                                                       B bone, ModelRuntimeData<B> data, GroupLocator groupLocator) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(context, poseStack, bone, cast(data));
    }

    public <B extends Model.Bone<B>> void visitPostBone(RenderParams renderParams, ModelVisitContext context,
                                                        B group, ModelRuntimeData<B> data, GroupLocator groupLocator) {
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

    public <B extends Model.Bone<B>> void visitLocator(
            RenderParams renderParams, ModelVisitContext context, B bone, LocatorEntry locator, ModelRuntimeData<B> data
    ) {

    }

    protected static <B extends Model.Bone<B>> void applyBoneTranslate(
            ModelVisitContext context, PoseStack poseStack, B bone, ModelRuntimeData<B> data
    ) {
        context.<Int2ObjectMap<PoseStack.Pose>>orCreate("bones", new Int2ObjectOpenHashMap<>()).compute(bone.id(), (n, pose) -> {
            if (pose == null) {
                PoseStack.Pose last = poseStack.last();
                Matrix4f m4 = last.pose();

                m4.translate(data.position(bone));

                var renderPivot = data.pivot(bone);
                m4.translate(renderPivot);

                var rotation = data.rotation(bone);

                last.normal().rotateZYX(rotation.z(), rotation.y(), rotation.x());
                m4.rotateZYX(rotation.z(), rotation.y(), rotation.x());

                var scale = data.scale(bone);
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
