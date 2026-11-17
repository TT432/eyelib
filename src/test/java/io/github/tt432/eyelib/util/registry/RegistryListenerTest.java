package io.github.tt432.eyelib.util.registry;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注册表类型化变更事件（G5）：写入语义、载荷形状、监听器隔离与注销。
 *
 * @author TT432
 */
class RegistryListenerTest {

    private static Registry<String> registry() {
        return new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
    }

    @Test
    void singlePutPublishesEntryChangeWithGeneration() {
        Registry<String> registry = registry();
        List<RegistryChange<String>> changes = new ArrayList<>();
        registry.addListener(changes::add);

        registry.put("a", "A");

        assertEquals(1, changes.size());
        RegistryChange<String> change = changes.get(0);
        assertEquals(RegistryChange.Kind.ENTRY_PUT, change.kind());
        assertEquals("a", change.id());
        assertEquals("A", change.value());
        assertEquals(registry.generation(), change.generation());
        assertEquals("A", change.snapshot().get("a"));
    }

    @Test
    void removePublishesEntryRemovedWithNullValue() {
        Registry<String> registry = registry();
        registry.put("a", "A");
        List<RegistryChange<String>> changes = new ArrayList<>();
        registry.addListener(changes::add);

        registry.remove("a");

        assertEquals(1, changes.size());
        assertEquals(RegistryChange.Kind.ENTRY_REMOVED, changes.get(0).kind());
        assertNull(changes.get(0).value());
        assertNull(changes.get(0).snapshot().get("a"));
    }

    @Test
    void removingMissingEntryPublishesNothing() {
        Registry<String> registry = registry();
        List<RegistryChange<String>> changes = new ArrayList<>();
        registry.addListener(changes::add);

        long before = registry.generation();
        registry.remove("missing");

        assertTrue(changes.isEmpty());
        assertEquals(before, registry.generation());
    }

    @Test
    void bulkReplacePublishesSingleBulkEventWithFullSnapshot() {
        Registry<String> registry = registry();
        List<RegistryChange<String>> changes = new ArrayList<>();
        registry.addListener(changes::add);

        registry.putAll(Map.of("a", "A", "b", "B"));

        // 批量替换没有逐条明细，只有一次 BULK_REPLACED + 完整快照（订阅者需整表重读）
        assertEquals(1, changes.size());
        assertEquals(RegistryChange.Kind.BULK_REPLACED, changes.get(0).kind());
        assertNull(changes.get(0).id());
        assertEquals(2, changes.get(0).snapshot().all().size());

        registry.replaceAll(Map.of("c", "C"));
        assertEquals(2, changes.size());
        assertEquals(RegistryChange.Kind.BULK_REPLACED, changes.get(1).kind());
        assertEquals(1, changes.get(1).snapshot().all().size());
    }

    @Test
    void listenerFailingDoesNotAffectOthersOrWrite() {
        Registry<String> registry = registry();
        List<String> seen = new ArrayList<>();
        registry.addListener(change -> {
            throw new IllegalStateException("boom");
        });
        registry.addListener(change -> seen.add(change.id()));

        registry.put("a", "A");

        assertEquals(List.of("a"), seen);
        assertEquals("A", registry.get("a"));
    }

    @Test
    void closedListenerStopsReceivingAndCloseIsIdempotent() throws Exception {
        Registry<String> registry = registry();
        List<RegistryChange<String>> changes = new ArrayList<>();
        AutoCloseable handle = registry.addListener(changes::add);

        registry.put("a", "A");
        handle.close();
        handle.close();
        registry.put("b", "B");

        assertEquals(1, changes.size());
        assertEquals(0, registry.listenerCount());
    }

    @Test
    void listenerAddedDuringDispatchDoesNotReceiveCurrentEvent() {
        Registry<String> registry = registry();
        List<String> later = new ArrayList<>();
        registry.addListener(change -> registry.addListener(c -> later.add(c.id())));

        registry.put("a", "A");

        assertTrue(later.isEmpty());
        // 分发快照取的是本轮写入前的监听器列表：新监听器从下一次变更起生效
        assertEquals(2, registry.listenerCount());
    }

    @Test
    void generationMonotonicAcrossWrites() {
        Registry<String> registry = registry();
        long start = registry.generation();
        registry.put("a", "A");
        long afterPut = registry.generation();
        registry.putAll(Map.of("b", "B"));
        long afterBulk = registry.generation();

        assertTrue(afterPut > start);
        assertTrue(afterBulk > afterPut);
        assertEquals(2, registry.snapshot().all().size());
    }
}
