package io.github.tt432.eyelib.client.render.sync;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelib.util.ResourceLocations;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RenderSyncApplyOpsTest {
    @Test
    void collectSerializableModelInfoFiltersOutNonSerializableEntries() {
        ModelComponent.SerializableInfo info = new ModelComponent.SerializableInfo(
                "test:model",
                ResourceLocations.of("eyelib", "textures/test"),
                ResourceLocations.of("eyelib", "render/solid")
        );

        ModelComponent serializable = new ModelComponent();
        serializable.setInfo(info);
        ModelComponent nonSerializable = new ModelComponent();

        List<RenderModelSyncPayload> payload = RenderSyncApplyOps.collectSerializableModelInfo(List.of(nonSerializable, serializable));

        assertEquals(1, payload.size());
        assertEquals("test:model", payload.get(0).model());
        assertEquals("eyelib:textures/test", payload.get(0).texture());
        assertEquals("eyelib:render/solid", payload.get(0).renderType());
    }

    @Test
    void replaceModelComponentsClearsAndRebuildsFromPayload() {
        RenderModelSyncPayload firstInfo = new RenderModelSyncPayload(
                "test:model_1",
                "eyelib:textures/one",
                "eyelib:render/solid"
        );
        RenderModelSyncPayload secondInfo = new RenderModelSyncPayload(
                "test:model_2",
                "eyelib:textures/two",
                "eyelib:render/cutout"
        );

        ModelComponent old = new ModelComponent();
        old.setInfo(new ModelComponent.SerializableInfo(
                firstInfo.model(),
                ResourceLocations.of(firstInfo.texture()),
                ResourceLocations.of(firstInfo.renderType())
        ));
        List<ModelComponent> target = new ArrayList<>();
        target.add(old);

        RenderSyncApplyOps.replaceModelComponents(target, List.of(firstInfo, secondInfo), payload -> new ModelComponent.SerializableInfo(
                payload.model(),
                ResourceLocations.of(payload.texture()),
                ResourceLocations.of(payload.renderType())
        ));

        ModelComponent.SerializableInfo rebuiltFirst = target.get(0).getSerializableInfo();
        ModelComponent.SerializableInfo rebuiltSecond = target.get(1).getSerializableInfo();

        assertNotNull(rebuiltFirst);
        assertNotNull(rebuiltSecond);
        assertEquals(2, target.size());
        assertEquals(firstInfo.model(), rebuiltFirst.model());
        assertEquals(firstInfo.texture(), rebuiltFirst.texture().toString());
        assertEquals(firstInfo.renderType(), rebuiltFirst.renderType().toString());
        assertEquals(secondInfo.model(), rebuiltSecond.model());
        assertEquals(secondInfo.texture(), rebuiltSecond.texture().toString());
        assertEquals(secondInfo.renderType(), rebuiltSecond.renderType().toString());
        assertNotSame(old, target.get(0));
    }

    @Test
    void applyAnimationInfoForwardsPayloadToApplier() {
        AnimationComponent.SerializableInfo info = new AnimationComponent.SerializableInfo(
                Map.of("controller.main", "animation.walk"),
                Map.of("controller.main", MolangValue.ONE)
        );
        AtomicReference<AnimationComponent.SerializableInfo> applied = new AtomicReference<>();

        RenderSyncApplyOps.applyAnimationInfo(applied::set, info);

        assertSame(info, applied.get());
    }
}
