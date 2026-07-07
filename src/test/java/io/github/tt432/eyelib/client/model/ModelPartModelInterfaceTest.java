package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.VisibleBox;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelPartModelInterfaceTest {
    @Test
    void boneAccessorsReturnExpectedValues() {
        var childPart = new ModelPart(List.of(), Map.of());
        var rootPart = new ModelPart(List.of(), Map.of("child", childPart));
        var model = new ModelPartModel("test", rootPart);

        assertEquals(1, model.allBones().size());
        Model.Bone bone = model.allBones().values().iterator().next();

        assertEquals(-1, bone.parent());
        assertTrue(bone.id() >= 0);
        assertNull(bone.binding());
        assertNull(bone.material());
        assertTrue(bone.children().isEmpty());
        assertTrue(bone.cubes().isEmpty());
        assertNotNull(bone.locator());
        assertTrue(bone.locator().children().isEmpty());
        assertFalse(bone.reset());
        assertTrue(bone.textureMeshes().isEmpty());

        assertEquals(0, bone.pivot().x());
        assertEquals(0, bone.pivot().y());
        assertEquals(0, bone.pivot().z());
        assertEquals(0, bone.rotation().x());
        assertEquals(0, bone.position().x());
        assertEquals(1, bone.scale().x());
        assertEquals(1, bone.scale().y());
        assertEquals(1, bone.scale().z());
    }

    @Test
    void modelAccessorsImplementModelContract() {
        var rootPart = new ModelPart(List.of(), Map.of());
        var model = new ModelPartModel("test", rootPart);

        assertEquals("test", model.name());
        assertTrue(model.toplevelBones().isEmpty());
        assertTrue(model.allBones().isEmpty());
        assertNotNull(model.locator());
        assertTrue(model.locator().groupLocatorMap().isEmpty());
        assertEquals(VisibleBox.EMPTY, model.visibleBox());
    }
}
