package io.github.tt432.eyelib.model;

import io.github.tt432.eyelib.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoneInterfaceTest {
    private static Model.Bone createSampleBone() {
        return Model.Bone.of(
                1, 0,
                new Vector3f(1, 2, 3),
                new Vector3f(4, 5, 6),
                new Vector3f(7, 8, 9),
                new Vector3f(2),
                "binding",
                new Int2ObjectOpenHashMap<>(),
                List.of(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of()),
                true,
                "material",
                List.of()
        );
    }

    @Test
    void withIdReturnsNewBoneWithChangedId() {
        var bone = createSampleBone();
        var result = Model.Bone.withId(bone, 99);

        assertEquals(99, result.id());
        assertEquals(bone.parent(), result.parent());
        assertSame(bone.pivot(), result.pivot());
        assertSame(bone.cubes(), result.cubes());
        assertSame(bone.locator(), result.locator());
    }

    @Test
    void withParentReturnsNewBoneWithChangedParent() {
        var bone = createSampleBone();
        var result = Model.Bone.withParent(bone, 88);

        assertEquals(88, result.parent());
        assertEquals(bone.id(), result.id());
        assertSame(bone.pivot(), result.pivot());
        assertSame(bone.children(), result.children());
    }

    @Test
    void withCubesReturnsNewBoneWithChangedCubes() {
        var bone = createSampleBone();
        var newCubes = List.<Model.Cube>of();
        var result = Model.Bone.withCubes(bone, newCubes);

        assertSame(newCubes, result.cubes());
        assertEquals(bone.id(), result.id());
        assertEquals(bone.parent(), result.parent());
    }

    @Test
    void withChildrenReturnsNewBoneWithChangedChildren() {
        var bone = createSampleBone();
        Int2ObjectMap<Model.Bone> newChildren = new Int2ObjectOpenHashMap<>();
        var result = Model.Bone.withChildren(bone, newChildren);

        assertSame(newChildren, result.children());
        assertEquals(bone.id(), result.id());
        assertSame(bone.cubes(), result.cubes());
    }
}
