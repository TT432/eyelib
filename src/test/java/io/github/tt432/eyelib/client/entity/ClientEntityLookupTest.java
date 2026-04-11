package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientEntityLookupTest {
    @AfterEach
    void tearDown() {
        ClientEntityManager.writePort().clear();
    }

    @Test
    void getReadsClientEntityByPlatformFreeStringIdentifier() {
        BrClientEntity clientEntity = testEntity();
        ClientEntityManager.writePort().put(clientEntity.identifier(), clientEntity);

        assertSame(clientEntity, ClientEntityLookup.get("eyelib:test_entity"));
        assertNull(ClientEntityLookup.get("eyelib:missing"));
    }

    @Test
    void managerNameExposesUnderlyingClientEntityManagerName() {
        assertEquals("ClientEntityManager", ClientEntityLookup.managerName());
    }

    private static BrClientEntity testEntity() {
        return new BrClientEntity(
                "eyelib:test_entity",
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
