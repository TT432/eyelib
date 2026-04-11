package io.github.tt432.eyelib.capability.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderControllerComponentTextureStateTest {
    @Test
    void textureStateChangeMarksExistingSlotForReloadUntilUploaded() {
        RenderControllerComponent component = new RenderControllerComponent();
        RenderControllerComponent.Slot slot = component.syncSlot(0, null);

        assertFalse(slot.needsTextureReload());

        RenderControllerComponent.onTextureStateChanged();
        assertTrue(slot.needsTextureReload());

        slot.markTextureUploaded();
        assertFalse(slot.needsTextureReload());
    }
}
