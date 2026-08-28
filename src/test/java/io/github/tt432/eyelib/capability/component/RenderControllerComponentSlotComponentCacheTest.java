package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangMapEntry;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opt18-A 组件级值键缓存契约：动态表达式逐帧求值保留（molang 语义），
 * 解析值全等时复用组件实例序（零下游重建），任一键分量变化即重建并应用新值。
 */
class RenderControllerComponentSlotComponentCacheTest {

    private static Model modelWithBones(String... names) {
        Int2ObjectOpenHashMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        for (String name : names) {
            int id = GlobalBoneIdHandler.get(name);
            bones.put(id, Model.Bone.of(id, -1, new Vector3f(), new Vector3f(), new Vector3f(),
                    new Vector3f(1), null, new Int2ObjectOpenHashMap<>(), List.of(),
                    new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())));
        }
        return Model.of("geometry.test", bones);
    }

    private static BrClientEntity entity() {
        return new BrClientEntity("minecraft:test",
                Map.of("default", "entity_translucent"),
                Map.of("default", "textures/entity/test"),
                Map.of("default", "geometry.test"),
                Map.of(), Map.of(), Map.of(),
                List.of("controller.render.test"), Optional.empty());
    }

    private static RenderControllerEntry rc(Map<String, MolangValue> partVisibility) {
        return new RenderControllerEntry(
                new MolangValue("Geometry.default"),
                List.of(new MolangValue("Texture.default")),
                Map.of(),
                List.of(new MolangMapEntry("*", new MolangValue("Material.default"))),
                partVisibility,
                false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    void equalEvaluatedValuesReuseSameComponentInstances() {
        RenderControllerEntry rc = rc(Map.of());
        BrClientEntity entity = entity();
        Model model = modelWithBones("body");
        MolangScope scope = new MolangScope();
        RenderControllerEntry.initStaticScope(scope, entity);

        RenderControllerComponent component = new RenderControllerComponent();
        RenderControllerComponent.Slot slot = component.syncSlot(0, rc);

        List<ModelComponent> first = rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>());
        assertFalse(first.isEmpty());
        // 值全等 → 同一实例序（零重建）
        assertSame(first, rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>()));
        assertSame(first, rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>()));
    }

    @Test
    void partVisibilityFlipRebuildsAndApplies() {
        RenderControllerEntry rc = rc(Map.of("hat", new MolangValue("v.show_hat")));
        BrClientEntity entity = entity();
        Model model = modelWithBones("body", "hat");
        MolangScope scope = new MolangScope();
        RenderControllerEntry.initStaticScope(scope, entity);
        scope.set("variable.show_hat", 1F);

        RenderControllerComponent component = new RenderControllerComponent();
        RenderControllerComponent.Slot slot = component.syncSlot(0, rc);

        List<ModelComponent> visible = rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>());
        int hatId = GlobalBoneIdHandler.get("hat");
        int bodyId = GlobalBoneIdHandler.get("body");
        assertTrue(visible.get(0).getPartVisibility().getOrDefault(hatId, false));
        assertTrue(visible.get(0).getPartVisibility().getOrDefault(bodyId, false));
        assertSame(visible, rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>()));

        // pv 表达式翻转为假 → 重建，hat 隐藏、body 不受影响
        scope.set("variable.show_hat", 0F);
        List<ModelComponent> hidden = rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>());
        assertNotSame(visible, hidden);
        assertFalse(hidden.get(0).getPartVisibility().getOrDefault(hatId, true));
        assertTrue(hidden.get(0).getPartVisibility().getOrDefault(bodyId, false));

        // 翻转回去 → 再次重建并恢复可见
        scope.set("variable.show_hat", 1F);
        List<ModelComponent> shownAgain = rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>());
        assertNotSame(hidden, shownAgain);
        assertTrue(shownAgain.get(0).getPartVisibility().getOrDefault(hatId, false));
        assertSame(shownAgain, rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>()));
    }

    @Test
    void modelVersionChangeInvalidatesCache() {
        RenderControllerEntry rc = rc(Map.of());
        BrClientEntity entity = entity();
        Model model = modelWithBones("body");
        MolangScope scope = new MolangScope();
        RenderControllerEntry.initStaticScope(scope, entity);

        RenderControllerComponent component = new RenderControllerComponent();
        RenderControllerComponent.Slot slot = component.syncSlot(0, rc);

        List<ModelComponent> first = rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>());
        assertNotSame(first, rc.setupModel(scope, entity, List.of(model), 2, slot, new ArrayList<>()));
    }

    @Test
    void textureStateChangeBypassesCache() {
        RenderControllerEntry rc = rc(Map.of());
        BrClientEntity entity = entity();
        Model model = modelWithBones("body");
        MolangScope scope = new MolangScope();
        RenderControllerEntry.initStaticScope(scope, entity);

        RenderControllerComponent component = new RenderControllerComponent();
        RenderControllerComponent.Slot slot = component.syncSlot(0, rc);

        List<ModelComponent> first = rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>());
        RenderControllerComponent.onTextureStateChanged();
        try {
            assertNotSame(first, rc.setupModel(scope, entity, List.of(model), 1, slot, new ArrayList<>()));
        } finally {
            RenderControllerComponent.onTextureStateChanged();
        }
    }
}
