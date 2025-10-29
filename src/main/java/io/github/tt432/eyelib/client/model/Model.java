package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.chin.util.Lists;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.locator.ModelLocator;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * @author TT432
 */
public interface Model {
    String name();

    Int2ObjectMap<? extends Bone> toplevelBones();

    ModelRuntimeData<?, ?, ?> data();

    ModelLocator locator();

    default <D extends ModelRuntimeData<Model.Bone, ?, D>> void accept(RenderParams params, ModelVisitContext context, D infos, ModelVisitor visitor) {
        visitor.visitPreModel(params, context, infos, this);

        for (var toplevelBone : toplevelBones().values()) {
            toplevelBone.accept(params, context, infos, locator().getGroup(toplevelBone.id()), visitor);
        }

        visitor.visitPostModel(params, context, infos, this);
    }

    interface Bone {
        int id();

        MolangValue binding();

        Int2ObjectMap<? extends Bone> children();

        List<? extends Cube> cubes();

        default <D extends ModelRuntimeData<Model.Bone, ?, D>> void accept(RenderParams params, ModelVisitContext context, D infos, GroupLocator groupLocator, ModelVisitor visitor) {
            ModelTransformer<Bone, D> transformer = infos.transformer();
            visitor.visitPreBone(params, context, this, infos, groupLocator, transformer);

            if (null == groupLocator) return;

            List<LocatorEntry> cubes = groupLocator.cubes();

            if (cubes != null) {
                PoseStack poseStack = params.poseStack();

                cubes.forEach(locator -> {
                    poseStack.pushPose();

                    PoseStack.Pose last1 = poseStack.last();
                    Matrix4f pose = last1.pose();
                    pose.translate(locator.offset());
                    pose.rotateZYX(locator.rotation());
                    last1.normal().rotateZYX(locator.rotation());

                    visitor.visitLocator(params, context, this, locator, infos, transformer);

                    poseStack.popPose();
                });
            }

            AtomicBoolean render = new AtomicBoolean(params.partVisibility().isEmpty());
            // todo 删除这部分
            params.partVisibility().forEach((k, v) -> {
                if (!Pattern.compile(k.replace("*", ".*")).matcher(GlobalBoneIdHandler.get(id())).matches() || v) {
                    render.set(true);
                }
            });

            if (render.get()) {
                for (int i = 0; i < cubes().size(); i++) {
                    cubes().get(i).accept(params, context, visitor);
                }
            }

            for (var child : children().values()) {
                child.accept(params, context, infos, groupLocator.getChild(child.id()), visitor);
            }

            visitor.visitPostBone(params, context, this, infos, groupLocator.getChild(id()), transformer);
        }
    }

    interface Cube {
        default List<List<Vector3f>> vertexes() {
            return Lists.asList(faceCount(), i -> Lists.asList(pointsPerFace(), j -> new Vector3f(positionX(i, j), positionY(i, j), positionZ(i, j))));
        }

        default List<List<Vector2f>> uvs() {
            return Lists.asList(faceCount(), i -> Lists.asList(pointsPerFace(), j -> new Vector2f(uvU(i, j), uvV(i, j))));
        }

        default List<Vector3f> normals() {
            return Lists.asList(faceCount(), i -> new Vector3f(normalX(i), normalY(i), normalZ(i)));
        }

        interface ConstCube extends Cube {
            @Override
            List<List<Vector3f>> vertexes();

            @Override
            List<List<Vector2f>> uvs();

            @Override
            List<Vector3f> normals();

            @Override
            default float positionX(int faceIndex, int pointIndex) {
                return vertexes().get(faceIndex).get(pointIndex).x();
            }

            @Override
            default float positionY(int faceIndex, int pointIndex) {
                return vertexes().get(faceIndex).get(pointIndex).y();
            }

            @Override
            default float positionZ(int faceIndex, int pointIndex) {
                return vertexes().get(faceIndex).get(pointIndex).z();
            }

            @Override
            default float uvU(int faceIndex, int pointIndex) {
                return uvs().get(faceIndex).get(pointIndex).x();
            }

            @Override
            default float uvV(int faceIndex, int pointIndex) {
                return uvs().get(faceIndex).get(pointIndex).y();
            }

            @Override
            default float normalX(int faceIndex) {
                return normals().get(faceIndex).x();
            }

            @Override
            default float normalY(int faceIndex) {
                return normals().get(faceIndex).y();
            }

            @Override
            default float normalZ(int faceIndex) {
                return normals().get(faceIndex).z();
            }
        }

        int faceCount();

        int pointsPerFace();

        float positionX(int faceIndex, int pointIndex);

        float positionY(int faceIndex, int pointIndex);

        float positionZ(int faceIndex, int pointIndex);

        float uvU(int faceIndex, int pointIndex);

        float uvV(int faceIndex, int pointIndex);

        float normalX(int faceIndex);

        float normalY(int faceIndex);

        float normalZ(int faceIndex);

        default void accept(RenderParams params, ModelVisitContext context, ModelVisitor visitor) {
            visitor.visitCube(params, context, this);
        }
    }
}
