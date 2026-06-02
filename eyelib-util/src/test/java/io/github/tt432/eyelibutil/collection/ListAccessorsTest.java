package io.github.tt432.eyelibutil.collection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** @author TT432 */
class ListAccessorsTest {
    @Test
    void firstReturnsFirstElement() {
        List<String> values = List.of("first", "middle", "last");

        assertEquals("first", ListAccessors.first(values));
    }

    @Test
    void lastReturnsLastElement() {
        List<String> values = List.of("first", "middle", "last");

        assertEquals("last", ListAccessors.last(values));
    }

    @Test
    void listsAsListKeepsLazyFastUtilBackedView() {
        List<String> values = Lists.asList(3, index -> "value-" + index);

        assertEquals(3, values.size());
        assertEquals("value-0", values.get(0));
        assertEquals("value-2", values.get(2));
    }

    @Test
    void entryStreamsCollectsWithJdkCollectorsSemantics() {
        Map<String, Integer> values = List.of(Map.entry("a", 1), Map.entry("b", 2))
                .stream()
                .collect(EntryStreams.collectSequenced());

        assertEquals(List.of("a", "b"), List.copyOf(values.keySet()));
        assertEquals(2, values.get("b"));
    }
}