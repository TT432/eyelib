package io.github.tt432.eyelib.bridge.molang.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code query.is_item_name_any} 名字匹配纯逻辑（BE 规范：'' 匹配空槽、命名空间可省略、大小写不敏感）。
 *
 * @author TT432
 */
class ItemNameMatcherTest {
    @Test
    void emptyStringCandidateMatchesOnlyEmptySlot() {
        assertTrue(ItemNameMatcher.matches(null, ""));
        assertFalse(ItemNameMatcher.matches(null, "minecraft:air"));
        assertFalse(ItemNameMatcher.matches(null, "bow"));
    }

    @Test
    void emptyStringCandidateNeverMatchesAnOccupiedSlot() {
        assertFalse(ItemNameMatcher.matches("minecraft:bow", ""));
    }

    @Test
    void namespaceMayBeOmitted() {
        assertTrue(ItemNameMatcher.matches("minecraft:bow", "bow"));
        assertTrue(ItemNameMatcher.matches("minecraft:bow", "minecraft:bow"));
        assertFalse(ItemNameMatcher.matches("myaddon:bow", "bow"));
        assertTrue(ItemNameMatcher.matches("myaddon:bow", "myaddon:bow"));
    }

    @Test
    void nameComparisonIsCaseInsensitive() {
        assertTrue(ItemNameMatcher.matches("minecraft:bow", "Minecraft:Bow"));
        assertTrue(ItemNameMatcher.matches("minecraft:bow", "BOW"));
    }

    @Test
    void matchesAnyAcceptsAnyCandidate() {
        assertTrue(ItemNameMatcher.matchesAny("minecraft:bow", "dirt", "bow"));
        assertTrue(ItemNameMatcher.matchesAny(null, "dirt", ""));
        assertFalse(ItemNameMatcher.matchesAny("minecraft:bow", "dirt", "stone"));
        assertFalse(ItemNameMatcher.matchesAny("minecraft:bow"));
    }

    @Test
    void normalizeFillsDefaultNamespaceAndLowercases() {
        assertEquals("minecraft:bow", ItemNameMatcher.normalize("Bow"));
        assertEquals("myaddon:bow", ItemNameMatcher.normalize("MyAddon:Bow"));
    }
}
