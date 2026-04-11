package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClientEntityAssetRegistryTest {
    @AfterEach
    void tearDown() {
        ClientEntityManager.writePort().clear();
    }

    @Test
    void replaceClientEntitiesUsesEntityIdentifierAsStorageKey() {
        BrClientEntity stale = testEntity("eyelib:stale");
        ClientEntityManager.writePort().put(stale.identifier(), stale);

        BrClientEntity first = testEntity("eyelib:first");
        BrClientEntity second = testEntity("eyelib:second");
        ClientEntityAssetRegistry.replaceClientEntities(List.of(first, second));

        assertNull(ClientEntityManager.readPort().get("eyelib:stale"));
        assertEquals(first, ClientEntityManager.readPort().get("eyelib:first"));
        assertEquals(second, ClientEntityManager.readPort().get("eyelib:second"));
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
