package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** @author TT432 */
class AttachableAssetRegistryTest {
    @AfterEach
    void tearDown() {
        AttachableManager.INSTANCE.clear();
    }

    @Test
    void replaceAttachablesUsesEntityIdentifierAsStorageKey() {
        BrClientEntity stale = testAttachable("eyelib:stale_attachable");
        AttachableManager.INSTANCE.put(stale.identifier(), stale);

        BrClientEntity first = testAttachable("eyelib:first_attachable");
        BrClientEntity second = testAttachable("eyelib:second_attachable");
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        List.of(first, second).forEach(attachable -> flattened.put(attachable.identifier(), attachable));
        AttachableManager.INSTANCE.replaceAll(flattened);

        assertNull(AttachableManager.INSTANCE.get("eyelib:stale_attachable"));
        assertEquals(first, AttachableManager.INSTANCE.get("eyelib:first_attachable"));
        assertEquals(second, AttachableManager.INSTANCE.get("eyelib:second_attachable"));
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
