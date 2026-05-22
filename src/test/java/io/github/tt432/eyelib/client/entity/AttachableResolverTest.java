package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttachableResolverTest {

    @AfterEach
    void tearDown() {
        AttachableManager.writePort().clear();
    }

    @Test
    void resolveByItemIdReturnsAttachableWhenItemMatches() {
        BrClientEntity wrench = testAttachable("eyelib:wrench", Map.of("demo:wrench", "1.0"));
        BrClientEntity sword = testAttachable("eyelib:sword", Map.of("demo:sword", "1.0"));
        AttachableManager.writePort().put(wrench.identifier(), wrench);
        AttachableManager.writePort().put(sword.identifier(), sword);

        assertEquals(wrench, AttachableResolver.resolveByItemId("demo:wrench"));
        assertEquals(sword, AttachableResolver.resolveByItemId("demo:sword"));
    }

    @Test
    void resolveByItemIdReturnsNullWhenNoMatch() {
        BrClientEntity wrench = testAttachable("eyelib:wrench", Map.of("demo:wrench", "1.0"));
        AttachableManager.writePort().put(wrench.identifier(), wrench);

        assertNull(AttachableResolver.resolveByItemId("demo:nonexistent"));
    }

    @Test
    void resolveByItemIdReturnsNullWhenManagerIsEmpty() {
        assertNull(AttachableResolver.resolveByItemId("demo:anything"));
    }

    @Test
    void resolveByItemIdMatchesFirstAttachableWithItemKey() {
        BrClientEntity first = testAttachable("eyelib:a", Map.of("demo:shared", "1.0"));
        BrClientEntity second = testAttachable("eyelib:b", Map.of("demo:shared", "1.0"));
        AttachableManager.writePort().put(first.identifier(), first);
        AttachableManager.writePort().put(second.identifier(), second);

        assertEquals(first, AttachableResolver.resolveByItemId("demo:shared"));
    }

    @Test
    void resolveByItemIdHandlesMultipleItemsInOneAttachable() {
        BrClientEntity tool = testAttachable("eyelib:tool",
                Map.of("demo:pickaxe", "1.0", "demo:axe", "query.is_sneaking"));
        AttachableManager.writePort().put(tool.identifier(), tool);

        assertEquals(tool, AttachableResolver.resolveByItemId("demo:pickaxe"));
        assertEquals(tool, AttachableResolver.resolveByItemId("demo:axe"));
    }

    private static BrClientEntity testAttachable(String identifier, Map<String, String> item) {
        return new BrClientEntity(
                identifier,
                Optional.empty(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Optional.empty(),
                Optional.empty(),
                item,
                false
        );
    }
}
