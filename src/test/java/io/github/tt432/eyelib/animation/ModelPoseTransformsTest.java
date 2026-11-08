package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import io.github.tt432.eyelib.model.locator.LocatorEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelPoseTransformsTest {
    @Test
    void locatorPoseUsesRenderedEntityBonePivotRotationAndScaleOrder() {
        int rootId = 1;
        LocatorEntry locator = new LocatorEntry("effect", new Vector3f(2, 0, 0), new Vector3f());
        Model.Bone root = Model.Bone.of(
                rootId,
                -1,
                new Vector3f(1, 0, 0),
                new Vector3f(),
                new Vector3f(0, 1, 0),
                new Vector3f(1),
                null,
                new Int2ObjectOpenHashMap<>(),
                List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of(locator))
        );
        Int2ObjectOpenHashMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        bones.put(rootId, root);
        Model model = Model.of("geometry.anchor", bones);

        ModelRuntimeData data = new ModelRuntimeData();
        data.getData(rootId).rotation.set(0, 0, (float) (Math.PI / 2));
        data.getData(rootId).scale.set(2, 1, 1);

        Matrix4f pose = ModelPoseTransforms.resolveLocatorPose(
                model,
                data,
                "effect",
                new Matrix4f().translation(10, 20, 30)
        ).orElseThrow();
        Vector3f position = pose.getTranslation(new Vector3f());

        assertEquals(9F, position.x, 0.0001F);
        assertEquals(23F, position.y, 0.0001F);
        assertEquals(30F, position.z, 0.0001F);
    }

    @Test
    void bonePoseAccumulatesParentChainWithPivotRotationAndModelFlip() {
        int rootId = 11;
        int handId = 12;
        Model.Bone hand = Model.Bone.of(
                handId,
                rootId,
                new Vector3f(0.25F, 1.5F, 0F),
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1),
                null,
                new Int2ObjectOpenHashMap<>(),
                List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())
        );
        Int2ObjectOpenHashMap<Model.Bone> rootChildren = new Int2ObjectOpenHashMap<>();
        rootChildren.put(handId, hand);
        Model.Bone root = Model.Bone.of(
                rootId,
                -1,
                new Vector3f(0F, 1F, 0F),
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1),
                null,
                rootChildren,
                List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())
        );
        Int2ObjectOpenHashMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        bones.put(rootId, root);
        bones.put(handId, hand);
        Model model = Model.of("geometry.chain", bones);

        ModelRuntimeData data = new ModelRuntimeData();
        // 根骨骼绕 Z 转 90°：手骨骼 pivot 绕根 pivot 旋转，再整体过模型 Y 翻转
        data.getData(rootId).rotation.set(0, 0, (float) (Math.PI / 2));

        Matrix4f pose = ModelPoseTransforms.resolveBonePose(model, data, handId, new Matrix4f()).orElseThrow();

        // 关节位置 = 骨骼帧作用在 hand pivot 上：
        // (0,1,0) + Rz90·((0.25,1.5,0) - (0,1,0)) = (-0.5,1.25,0)，再过 Ry(180°) → (0.5,1.25,0)
        Vector3f position = pose.transformPosition(new Vector3f(0.25F, 1.5F, 0F));
        assertEquals(0.5F, position.x, 0.0001F);
        assertEquals(1.25F, position.y, 0.0001F);
        assertEquals(0F, position.z, 0.0001F);
    }

    @Test
    void bonePoseWithoutAnimationIsOnlyTheModelFlip() {
        int rootId = 21;
        int handId = 22;
        Model.Bone hand = Model.Bone.of(
                handId,
                rootId,
                new Vector3f(0.25F, 1.5F, 0F),
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1),
                null,
                new Int2ObjectOpenHashMap<>(),
                List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())
        );
        Model.Bone root = Model.Bone.of(
                rootId,
                -1,
                new Vector3f(0F, 1F, 0F),
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1),
                null,
                new Int2ObjectOpenHashMap<>(),
                List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())
        );
        Int2ObjectOpenHashMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        bones.put(rootId, root);
        bones.put(handId, hand);
        Model model = Model.of("geometry.static", bones);

        Matrix4f pose = ModelPoseTransforms.resolveBonePose(
                model,
                ModelRuntimeData.EMPTY,
                handId,
                new Matrix4f().translation(10, 20, 30)
        ).orElseThrow();

        // 无动画时骨骼链的 T(pivot)·T(-pivot) 全部抵消：根位姿平移不变，只有模型的 Y 翻转作用在方向上
        Vector3f position = pose.getTranslation(new Vector3f());
        assertEquals(10F, position.x, 0.0001F);
        assertEquals(20F, position.y, 0.0001F);
        assertEquals(30F, position.z, 0.0001F);

        Vector3f direction = pose.transformDirection(new Vector3f(1, 0, 0));
        assertEquals(-1F, direction.x, 0.0001F);
        assertEquals(0F, direction.y, 0.0001F);
        assertEquals(0F, direction.z, 0.0001F);
    }

    @Test
    void missingBoneDoesNotProduceAPose() {
        Model model = Model.of("geometry.empty", new Int2ObjectOpenHashMap<>());

        assertTrue(ModelPoseTransforms.resolveBonePose(model, ModelRuntimeData.EMPTY, 999, new Matrix4f())
                .isEmpty());
    }

    @Test
    void missingLocatorDoesNotProduceAPose() {
        Model model = Model.of("geometry.empty", new Int2ObjectOpenHashMap<>());

        assertTrue(ModelPoseTransforms.resolveLocatorPose(
                model,
                ModelRuntimeData.EMPTY,
                "missing",
                new Matrix4f()
        ).isEmpty());
    }
}
