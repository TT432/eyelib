package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.ClientEntityManager;
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
class ClientEntityAssetRegistryTest {
    @AfterEach
    void tearDown() {
        ClientEntityManager.INSTANCE.clear();
    }

    @Test
    void replaceClientEntitiesUsesEntityIdentifierAsStorageKey() {
        BrClientEntity stale = testEntity("eyelib:stale");
        ClientEntityManager.INSTANCE.put(stale.identifier(), stale);

        BrClientEntity first = testEntity("eyelib:first");
        BrClientEntity second = testEntity("eyelib:second");
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        List.of(first, second).forEach(entity -> flattened.put(entity.identifier(), entity));
        ClientEntityManager.INSTANCE.replaceAll(flattened);

        assertNull(ClientEntityManager.INSTANCE.get("eyelib:stale"));
        assertEquals(first, ClientEntityManager.INSTANCE.get("eyelib:first"));
        assertEquals(second, ClientEntityManager.INSTANCE.get("eyelib:second"));
    }

    private static BrClientEntity testEntity(String identifier) {
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
