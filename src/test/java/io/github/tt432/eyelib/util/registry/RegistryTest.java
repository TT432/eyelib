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
    void getWithNullIdReturnsNullEvenOnEmptySnapshot() {
        // 崩溃回归：空快照底层为 Map.of()，ImmutableCollections.MapN.get 对 null 键抛 NPE
        //（motions4mod CameraManager 每帧 get(null)，重载后快照为空时崩溃）。
        Registry<String> empty = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        assertNull(empty.get(null));

        Registry<String> nonEmpty = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        nonEmpty.put("entry", "value");
        assertNull(nonEmpty.get(null));
    }

    @Test
    void replaceAllPublishesReplacedEvent() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);

        registry.replaceAll(Map.of("entry", "value"));

        // 整表替换无逐条事件，但必须发布批量替换事件——否则只订阅
        // ManagerEntryChangedEvent 的缓存（烘焙模型/DFS/动画组件）永不失效
        assertEquals("TestRegistry", publisher.replacedManagerName);
        assertNull(publisher.entryName);
    }

    @Test
    void clearPublishesReplacedEvent() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);
        registry.put("entry", "value");

        registry.clear();

        assertTrue(registry.all().isEmpty());
        assertEquals("TestRegistry", publisher.replacedManagerName);
    }

    @Test
    void generationIncrementsOnEveryMutation() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        long initial = registry.generation();

        registry.put("a", "1");
        assertEquals(initial + 1, registry.generation());

        registry.putAll(Map.of("b", "2"));
        assertEquals(initial + 2, registry.generation());

        registry.replaceAll(Map.of("c", "3"));
        assertEquals(initial + 3, registry.generation());

        registry.remove("c");
        assertEquals(initial + 4, registry.generation());

        registry.clear();
        assertEquals(initial + 5, registry.generation());
    }

    @Test
    void generationUnchangedOnReadsAndNoOpMutations() {
        Registry<String> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        registry.put("a", "1");
        long generation = registry.generation();

        registry.get("a");
        registry.all();
        registry.names();
        registry.snapshot();
        registry.remove("missing");
        registry.removeAll(java.util.List.of("missing"));
        registry.putAll(Map.of());

        assertEquals(generation, registry.generation());
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

    @Test
    void removeDeletesEntryAndPublishesNullData() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);
        registry.put("entry", "value");

        registry.remove("entry");

        assertNull(registry.get("entry"));
        assertEquals("TestRegistry", publisher.managerName);
        assertEquals("entry", publisher.entryName);
        assertNull(publisher.entryData);
    }

    @Test
    void removeMissingKeyDoesNotPublish() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);

        registry.remove("missing");

        assertNull(publisher.managerName);
    }

    @Test
    void removeAllDeletesMultipleAndPublishesSingleReplaced() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);
        registry.put("a", "1");
        registry.put("b", "2");
        registry.put("c", "3");

        registry.removeAll(java.util.List.of("a", "b", "missing"));

        assertEquals(Map.of("c", "3"), registry.all());
        assertEquals("TestRegistry", publisher.replacedManagerName);
    }

    @Test
    void removeAllWithNoMatchingKeyDoesNotPublish() {
        RecordingPublisher publisher = new RecordingPublisher();
        Registry<String> registry = new Registry<>("TestRegistry", publisher);
        registry.put("a", "1");

        registry.removeAll(java.util.List.of("missing"));

        assertEquals(Map.of("a", "1"), registry.all());
        assertNull(publisher.replacedManagerName);
    }

    private static final class RecordingPublisher implements ManagerEventPublisher {
        private @Nullable String managerName;
        private @Nullable String entryName;
        private @Nullable Object entryData;
        private @Nullable String replacedManagerName;

        @Override
        public void publishManagerEntryChanged(String managerName, String entryName, Object entryData) {
            this.managerName = managerName;
            this.entryName = entryName;
            this.entryData = entryData;
        }

        @Override
        public void publishManagerReplaced(String managerName) {
            this.replacedManagerName = managerName;
        }
    }
}
