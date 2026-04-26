package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttachableAssetRegistryTest {
    @AfterEach
    void tearDown() {
        AttachableManager.writePort().clear();
    }

    @Test
    void replaceAttachablesUsesEntityIdentifierAsStorageKey() {
        BrClientEntity stale = testAttachable("eyelib:stale_attachable");
        AttachableManager.writePort().put(stale.identifier(), stale);

        BrClientEntity first = testAttachable("eyelib:first_attachable");
        BrClientEntity second = testAttachable("eyelib:second_attachable");
        AttachableAssetRegistry.replaceAttachables(List.of(first, second));

        assertNull(AttachableManager.readPort().get("eyelib:stale_attachable"));
        assertEquals(first, AttachableManager.readPort().get("eyelib:first_attachable"));
        assertEquals(second, AttachableManager.readPort().get("eyelib:second_attachable"));
    }

    private static BrClientEntity testAttachable(String identifier) {
        return new BrClientEntity(
                identifier,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Optional.empty()
        );
    }
}
