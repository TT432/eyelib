package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
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

import java.util.List;

/**
 * @author TT432
 */
@UtilityClass
public class ModelRenderer {
    private static final float R180 = 180 * EyeMath.DEGREES_TO_RADIANS;

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static <R extends ModelRuntimeData<?, ?, R>> void render(RenderParams renderParams, Model model, R infos,
                                                                    @Nullable BrModelTextures.TwoSideInfoMap map,
                                                                    ModelRenderVisitorList visitors) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        pose.rotateY(R180);
        Matrix3f normal = last.normal();
        normal.rotateY(R180);

        visitors.visitors().forEach(v -> v.visitModel(renderParams));

        for (var toplevelBone : model.toplevelBones().values()) {
            renderBone(renderParams, map, visitors, cast(infos), model, toplevelBone, model.locator().getGroup(toplevelBone.name()));
        }

        poseStack.popPose();
    }

    @SuppressWarnings("unchecked")
    private static <G extends Model.Bone, R extends ModelRuntimeData<G, Object, R>> void renderBone(
            RenderParams renderParams, @Nullable BrModelTextures.TwoSideInfoMap map,
            ModelRenderVisitorList visitors, R infos, Model model, G bone, GroupLocator locatorsGroup
    ) {
        PoseStack poseStack = renderParams.poseStack();
        poseStack.pushPose();

        ModelTransformer<G, R> transformer = infos.transformer();

        applyBoneTranslate(poseStack, bone, infos, transformer);

        visitors.visitors().forEach(visitor -> visitor.visitBone(renderParams, bone, infos, transformer));

        visitLocators(renderParams, visitors, bone, poseStack, bone, infos, transformer, locatorsGroup);

        for (int i = 0; i < bone.cubes().size(); i++) {
            renderCube(renderParams, visitors, bone.cubes().get(i), map == null || map.isTwoSide(bone.name(), i));
        }

        for (var child : bone.children().values()) {
            renderBone(renderParams, map, visitors, infos, model, (G) child, locatorsGroup.getChild(child.name()));
        }

        poseStack.popPose();
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

    private static void renderCube(RenderParams renderParams, ModelRenderVisitorList visitors,
                                   Model.Cube cube, boolean needTwoSide) {
        visitors.visitors().forEach(visitor -> visitor.visitCube(renderParams, cube));

        for (int i = 0; i < cube.vertexes().size(); i++) {
            List<Vector3fc> vertexes = cube.vertexes().get(i);
            List<Vector2fc> uvs = cube.uvs().get(i);
            Vector3fc normal = cube.normals().get(i);

            visitors.visitors().forEach(visitor -> visitor.visitFace(renderParams, cube, vertexes, uvs, normal));

            for (int vertexIndex = 0; vertexIndex < vertexes.size(); vertexIndex++) {
                Vector3fc vertex = vertexes.get(vertexIndex);
                Vector2fc uv = uvs.get(vertexIndex);

                visitors.visitors().forEach(visitor -> visitor.visitVertex(renderParams, cube, vertex, uv, normal));
            }

            if (needTwoSide) {
                for (int vertexIndex = vertexes.size() - 1; vertexIndex >= 0; vertexIndex--) {
                    Vector3fc vertex = vertexes.get(vertexIndex);
                    Vector2fc uv = uvs.get(vertexIndex);

                    visitors.visitors().forEach(visitor -> visitor.visitVertex(renderParams, cube, vertex, uv, normal));
                }
            }
        }
    }
}
