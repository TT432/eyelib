package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;

import io.github.tt432.eyelibimporter.model.Model;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientEntityRuntimeDataTest {
    @Test
    void syncUsesLookupPortToResolveGeometryModels() {
        Model resolved = new Model("geometry.test", new Int2ObjectOpenHashMap<>());
        ClientEntityRuntimeData runtimeData = new ClientEntityRuntimeData(modelName ->
                "geometry.test".equals(modelName) ? resolved : null);

        BrClientEntity clientEntity = new BrClientEntity(
                "eyelib:test",
                Map.of(),
                Map.of(),
                Map.of("default", "geometry.test"),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Optional.empty()
        );

        assertTrue(runtimeData.sync(clientEntity));
        List<Model> models = new ArrayList<>(runtimeData.models());
        assertEquals(1, models.size());
        assertSame(resolved, models.get(0));
    }

    @Test
    void syncReturnsFalseForSameAppliedClientEntityInstance() {
        ClientEntityRuntimeData runtimeData = new ClientEntityRuntimeData(modelName -> null);
        BrClientEntity clientEntity = new BrClientEntity(
                "eyelib:test",
                Map.of(),
                Map.of(),
                Map.of("default", "geometry.missing"),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Optional.empty()
        );

        assertTrue(runtimeData.sync(clientEntity));
        assertFalse(runtimeData.sync(clientEntity));
    }

    @Test
    void syncToNullClearsResolvedModels() {
        Model resolved = new Model("geometry.test", new Int2ObjectOpenHashMap<>());
        ClientEntityRuntimeData runtimeData = new ClientEntityRuntimeData(modelName -> resolved);
        BrClientEntity clientEntity = new BrClientEntity(
                "eyelib:test",
                Map.of(),
                Map.of(),
                Map.of("default", "geometry.test"),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Optional.empty()
        );

        runtimeData.sync(clientEntity);
        assertTrue(runtimeData.sync(null));
        assertEquals(List.of(), new ArrayList<>(runtimeData.models()));
    }
}
