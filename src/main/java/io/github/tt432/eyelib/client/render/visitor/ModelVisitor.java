package io.github.tt432.eyelib.client.render.visitor;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.util.client.PoseHelper;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

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

    public void visitModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
        model.accept(params, context, infos, this);
    }

    public void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
        PoseStack poseStack = params.poseStack();
        poseStack.pushPose();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.rotateY(R180);
        Matrix3f normal = last.normal();
        normal.rotateY(R180);
    }

    public void visitPostModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
        PoseStack poseStack = params.poseStack();
        poseStack.popPose();
    }

    public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        applyBoneTranslate(context, poseStack, bone, cast(data));
    }

    public void visitPostBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, ModelRuntimeData data) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.popPose();
    }

    public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
        for (Model.Face face : cube.faces()) {
            visitFace(renderParams, context, cube, face.vertexes().stream().map(Model.Vertex::position).toList(),
                    face.vertexes().stream().map(Model.Vertex::uv).toList(), face.normal());

            for (Model.Vertex vertex : face.vertexes()) {
                visitVertex(renderParams, context, cube, vertex.position(), vertex.uv(), vertex.normal());
            }
        }
    }

    public void visitFace(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                          List<Vector3fc> vertexes, List<Vector2fc> uvs, Vector3fc normal) {

    }

    public void visitVertex(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                            Vector3fc vertex, Vector2fc uv, Vector3fc normal) {

    }

    public void visitLocator(
            RenderParams renderParams, ModelVisitContext context, Model.Bone bone, LocatorEntry locator, ModelRuntimeData data
    ) {

    }

    protected static void applyBoneTranslate(
            ModelVisitContext context, PoseStack poseStack, Model.Bone bone, ModelRuntimeData data
    ) {
        context.<Int2ObjectMap<PoseStack.Pose>>orCreate("bones", new Int2ObjectOpenHashMap<>()).compute(bone.id(), (n, pose) -> {
            if (pose == null) {
                PoseStack.Pose last = poseStack.last();
                Matrix4f m4 = last.pose();

                m4.translate(data.position(bone));

                var renderPivot = bone.pivot();
                m4.translate(renderPivot);

                var rotation = data.rotation(bone);

                last.normal().rotateZYX(rotation.z(), rotation.y(), rotation.x());
                m4.rotateZYX(rotation.z(), rotation.y(), rotation.x());

                var scale = data.scale(bone);
                poseStack.scale(scale.x(), scale.y(), scale.z());

                m4.translate(-renderPivot.x(), -renderPivot.y(), -renderPivot.z());
                return PoseHelper.copy(last);
            } else {
                Deque<PoseStack.Pose> stack = poseStack.poseStack;
                stack.removeLast();
                stack.addLast(pose);
                return pose;
            }
        });
    }
}
