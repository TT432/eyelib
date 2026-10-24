package io.github.tt432.eyelib.util.collection;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
