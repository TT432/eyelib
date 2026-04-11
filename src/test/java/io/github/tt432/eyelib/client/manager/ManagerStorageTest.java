package io.github.tt432.eyelib.client.manager;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManagerStorageTest {
    @Test
    void putAndGetAllDataReturnsSnapshot() {
        ManagerStorage<String> storage = new ManagerStorage<>();
        storage.put("first", "value");

        Map<String, String> snapshot = storage.getAllData();
        snapshot.put("second", "other");

        assertEquals("value", storage.get("first"));
        assertNull(storage.get("second"));
    }

    @Test
    void replaceAllOverwritesExistingEntries() {
        ManagerStorage<String> storage = new ManagerStorage<>();
        storage.put("stale", "old");

        LinkedHashMap<String, String> replacement = new LinkedHashMap<>();
        replacement.put("fresh", "new");
        storage.replaceAll(replacement);

        assertNull(storage.get("stale"));
        assertEquals("new", storage.get("fresh"));
    }

    @Test
    void clearRemovesAllEntries() {
        ManagerStorage<String> storage = new ManagerStorage<>();
        storage.put("entry", "value");

        storage.clear();

        assertEquals(Map.of(), storage.getAllData());
    }
}
