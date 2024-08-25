package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.RootModelPartModel;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.bedrock.BrCube;
import io.github.tt432.eyelib.client.model.bedrock.BrModel;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModelFormatTransformers {

    public record TranslationParams(
            HumanoidModel<?> original,
            PoseStack poseStack
    ) {
    }

    public static HumanoidModel<?> getArmorModel(BrModel model, HumanoidModel<?> original) {
        return new HumanoidModel<>(getVanillaPart(model, new TranslationParams(original, new PoseStack())));
    }

    public static ModelPart getVanillaPart(BrModel model, TranslationParams params) {
        PoseStack poseStack = params.poseStack;
        poseStack.pushPose();
        PoseStack.Pose last = poseStack.last();
        final float r1 = 180 * EyeMath.DEGREES_TO_RADIANS;
        last.normal().rotateZ(r1);
        last.pose().rotateZ(r1);
        last.pose().translate(0, -1.5F, 0);

        var result = new ModelPart(List.of(), model.toplevelBones().values().stream()
                .map(b -> Map.entry(b.name(), getVanillaPart(b, params)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        poseStack.popPose();
        return result;
    }

    private static void visitModelPart(ModelPart parent, BiConsumer<String, ModelPart> consumer) {
        parent.children.forEach((name, child) -> {
            consumer.accept(name, child);
            visitModelPart(child, consumer);
        });
    }

    public static ModelPart getVanillaPart(BrBone bone, TranslationParams params) {
        PoseStack poseStack = params.poseStack();
        poseStack.pushPose();
        AtomicReference<PartPose> pose = new AtomicReference<>(PartPose.ZERO);

        var last = poseStack.last();
        Vector3f pivot = bone.pivot();
        last.pose().translate(pivot);
        last.normal().rotateZYX(bone.rotation());
        last.pose().rotateZYX(bone.rotation());
        last.pose().translate(-pivot.x, -pivot.y, -pivot.z);

        if (params.original() instanceof RootModelPartModel model) {
            visitModelPart(model.getRootPart(), (name, child) -> {
                if (bone.name().equals(name)) {
                    PartPose initialPose = child.getInitialPose();
                    last.normal().rotateZYX(-initialPose.xRot, -initialPose.yRot, -initialPose.xRot);
                    last.pose().rotateZYX(-initialPose.xRot, -initialPose.yRot, -initialPose.xRot);
                    last.pose().translate(initialPose.x / 16, initialPose.y / 16, initialPose.z / 16);
                    pose.set(PartPose.offsetAndRotation(initialPose.x, initialPose.y, initialPose.z,
                            initialPose.xRot, initialPose.yRot, initialPose.zRot));
                }
            });
        }

        ModelPart result = new ModelPart(bone.cubes().stream().map(c -> getVanillaCube(c, params)).toList(),
                bone.children().values().stream().map(b -> Map.entry(b.name(), getVanillaPart(b, params)))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        result.setInitialPose(pose.get());
        result.resetPose();
        poseStack.popPose();
        return result;
    }

    private static final Set<Direction> ALL_VISIBLE = EnumSet.allOf(Direction.class);

    public static ModelPart.Cube getVanillaCube(BrCube cube, TranslationParams params) {
        int faceCount = cube.vertexes().size();
        ModelPart.Polygon[] polygons = new ModelPart.Polygon[faceCount];

        for (int i = 0; i < faceCount; i++) {
            List<Vector3fc> vertexes = cube.vertexes().get(i);
            var vertexCount = vertexes.size();
            List<Vector2fc> uvs = cube.uvs().get(i);
            var vertexArray = new ModelPart.Vertex[vertexCount];

            var wrapper = params.poseStack().last();
            for (int i1 = 0; i1 < vertexCount; i1++) {
                var uv = uvs.get(i);
                Vector3f pos = vertexes.get(i1).mulPosition(wrapper.pose(), new Vector3f()).mul(16);
                vertexArray[i1] = new ModelPart.Vertex(pos.x, pos.y, pos.z, uv.x(), uv.y()) {
                    @Override
                    public ModelPart.Vertex remap(float p_104385_, float p_104386_) {
                        return this;
                    }
                };
            }

            Vector3f normal = cube.normals().get(i).mul(wrapper.normal(), new Vector3f());
            polygons[i] = new ModelPart.Polygon(vertexArray, 1, 1, 1,
                    1, 1, 1, false,
                    Direction.getNearest(normal.x, normal.y, normal.z));
        }

        var result = new ModelPart.Cube(0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0,
                false, 0, 0, ALL_VISIBLE);

        System.arraycopy(polygons, 0, result.polygons, 0, result.polygons.length);

        return result;
    }
}

