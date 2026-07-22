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
