package io.github.tt432.eyelib.util.registry;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class RegistryTest {
    @Test
    void putThenGetReturnsValue() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);

        registry.put("entry", "value");

        assertEquals("value", registry.get("entry"));
    }

    @Test
    void putPublishesEventWithManagerNameAndEntry() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);

        registry.put("entry", "value");

        assertEquals("TestRegistry", publisher.managerName);
        assertEquals("entry", publisher.entryName);
        assertEquals("value", publisher.entryData);
    }

    @Test
    void replaceAllReplacesEntireSnapshot() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        registry.put("a", "1");
        registry.put("b", "2");

        registry.replaceAll(Map.of("c", "3"));

        assertEquals("3", registry.get("c"));
        assertNull(registry.get("a"));
        assertNull(registry.get("b"));
    }

    @Test
    void clearEmptiesRegistry() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        registry.put("entry", "value");

        registry.clear();

        assertNull(registry.get("entry"));
        assertTrue(registry.all().isEmpty());
    }

    @Test
    void snapshotIsImmutableAfterPut() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        registry.put("a", "1");

        RegistrySnapshot<String> snap = registry.snapshot();

        registry.put("b", "2");

        assertEquals("1", snap.get("a"));
        assertNull(snap.get("b"));
        assertEquals(1, snap.size());
    }

    @Test
    void allReturnsUnmodifiableMap() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        registry.put("entry", "value");

        Map<String, String> all = registry.all();

        assertThrows(UnsupportedOperationException.class, () -> all.put("hack", "x"));
    }

    @Test
    void getReturnsNullForUnregisteredId() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);

        assertNull(registry.get("missing"));
    }

    @Test
    void replaceAllDoesNotPublishEvent() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);

        registry.replaceAll(Map.of("entry", "value"));

        assertNull(publisher.managerName);
    }

    @Test
    void namesReturnsAllRegisteredIds() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        registry.put("a", "1");
        registry.put("b", "2");

        assertTrue(registry.names().contains("a"));
        assertTrue(registry.names().contains("b"));
        assertEquals(2, registry.names().size());
    }

    private static final class RecordingPublisher implements ManagerEventPublisher {
        private @Nullable String managerName;
        private @Nullable String entryName;
        private @Nullable Object entryData;

        @Override
        public void publishManagerEntryChanged(String managerName, String entryName, Object entryData) {
            this.managerName = managerName;
            this.entryName = entryName;
            this.entryData = entryData;
        }
    }
}
