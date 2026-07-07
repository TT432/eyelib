package io.github.tt432.eyelib.model;

import io.github.tt432.eyelib.model.locator.GroupLocator;
import io.github.tt432.eyelib.model.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 {@link Model} 的派生索引一致性与 CODEC 往返不变量。
 *
 * @author TT432
 */
class ModelIndexTest {
    @Test
    void toplevelBonesDerivedFromParentLinks() {
        Int2ObjectMap<Model.Bone> bones = sampleBones();
        Model model = Model.of("geometry.test", bones);

        assertEquals(2, model.toplevelBones().size());
        assertTrue(model.toplevelBones().containsKey(0));
        assertTrue(model.toplevelBones().containsKey(1));

        Model.Bone bone1 = model.allBones().get(1);
        assertEquals(1, bone1.children().size());
        assertTrue(bone1.children().containsKey(2));

        Model.Bone bone2 = model.allBones().get(2);
        assertEquals(1, bone2.children().size());
        assertTrue(bone2.children().containsKey(3));
    }

    @Test
    void locatorIndexReferencesToplevelBoneLocators() {
        Int2ObjectMap<Model.Bone> bones = sampleBones();
        ModelLocator locator = new ModelLocator(new Int2ObjectOpenHashMap<>());
        Model model = Model.of("geometry.test", bones, locator);

        Model.Bone bone0 = model.allBones().get(0);
        Model.Bone bone1 = model.allBones().get(1);
        Model.Bone bone2 = model.allBones().get(2);

        assertSame(bone0.locator(), locator.groupLocatorMap().get(0));
        assertSame(bone1.locator(), locator.groupLocatorMap().get(1));
        assertSame(bone2.locator(), bone1.locator().children().get(2));
        assertSame(model.allBones().get(3).locator(), bone2.locator().children().get(3));
    }

    @Test
    void prebuiltLocatorEntriesForBonesAreOverwrittenByBoneAuthoritativeSource() {
        Int2ObjectMap<Model.Bone> bones = sampleBones();
        GroupLocator staleTop0 = new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>());
        GroupLocator staleTop1 = new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>());

        Int2ObjectMap<GroupLocator> prebuilt = new Int2ObjectOpenHashMap<>();
        prebuilt.put(0, staleTop0);
        prebuilt.put(1, staleTop1);
        ModelLocator locator = new ModelLocator(prebuilt);

        Model model = Model.of("geometry.test", bones, locator);

        Model.Bone bone0 = model.allBones().get(0);
        Model.Bone bone1 = model.allBones().get(1);
        assertSame(bone0.locator(), locator.groupLocatorMap().get(0));
        assertSame(bone1.locator(), locator.groupLocatorMap().get(1));
        assertNotSame(staleTop0, locator.groupLocatorMap().get(0));
        assertNotSame(staleTop1, locator.groupLocatorMap().get(1));
    }

    /**
     * 4-bone 树：root0（孤立）、root1 → bone2 → bone3。
     * 所有 Bone 创建时 children 为空，由 SimpleModel 构造时填充。
     */
    private static Int2ObjectMap<Model.Bone> sampleBones() {
        Int2ObjectMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        bones.put(0, bone(0, -1));
        bones.put(1, bone(1, -1));
        bones.put(2, bone(2, 1));
        bones.put(3, bone(3, 2));
        return bones;
    }

    private static Model.Bone bone(int id, int parent) {
        return Model.Bone.of(
                id,
                parent,
                new Vector3f(),
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1),
                null,
                new Int2ObjectOpenHashMap<>(),
                List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), new ArrayList<>())
        );
    }
}
