package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class AttachableResolverTest {

    @AfterEach
    void tearDown() {
        AttachableManager.INSTANCE.clear();
    }

    @Test
    void resolveByItemIdReturnsAttachableWhenItemMatches() {
        BrClientEntity wrench = testAttachable("eyelib:wrench", Map.of("demo:wrench", "1.0"));
        BrClientEntity sword = testAttachable("eyelib:sword", Map.of("demo:sword", "1.0"));
        AttachableManager.INSTANCE.put(wrench.identifier(), wrench);
        AttachableManager.INSTANCE.put(sword.identifier(), sword);

        assertEquals(wrench, AttachableResolver.resolveByItemId("demo:wrench"));
        assertEquals(sword, AttachableResolver.resolveByItemId("demo:sword"));
    }

    @Test
    void resolveByItemIdReturnsNullWhenNoMatch() {
        BrClientEntity wrench = testAttachable("eyelib:wrench", Map.of("demo:wrench", "1.0"));
        AttachableManager.INSTANCE.put(wrench.identifier(), wrench);

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
        AttachableManager.INSTANCE.put(first.identifier(), first);
        AttachableManager.INSTANCE.put(second.identifier(), second);

        assertEquals(first, AttachableResolver.resolveByItemId("demo:shared"));
    }

    @Test
    void resolveByItemIdHandlesMultipleItemsInOneAttachable() {
        BrClientEntity tool = testAttachable("eyelib:tool",
                Map.of("demo:pickaxe", "1.0", "demo:axe", "query.is_sneaking"));
        AttachableManager.INSTANCE.put(tool.identifier(), tool);

        assertEquals(tool, AttachableResolver.resolveByItemId("demo:pickaxe"));
        assertEquals(tool, AttachableResolver.resolveByItemId("demo:axe"));
    }

    @Test
    void resolveByItemIdWithScopeMatchesConstantTrueCondition() {
        BrClientEntity wrench = testAttachable("eyelib:wrench", Map.of("demo:wrench", "1.0"));
        AttachableManager.INSTANCE.put(wrench.identifier(), wrench);

        MolangScope scope = new MolangScope();
        assertEquals(wrench, AttachableResolver.resolveByItemId("demo:wrench", scope));
    }

    @Test
    void resolveByItemIdWithScopeRejectsConstantFalseCondition() {
        BrClientEntity wrench = testAttachable("eyelib:wrench", Map.of("demo:wrench", "0.0"));
        AttachableManager.INSTANCE.put(wrench.identifier(), wrench);

        MolangScope scope = new MolangScope();
        assertNull(AttachableResolver.resolveByItemId("demo:wrench", scope));
    }

    @Test
    void resolveByItemIdWithScopeEvaluatesVariableCondition() {
        BrClientEntity flag = testAttachable("eyelib:flag", Map.of("demo:flag", "variable.flag"));
        AttachableManager.INSTANCE.put(flag.identifier(), flag);

        MolangScope truthy = new MolangScope();
        truthy.set("variable.flag", 1);
        assertEquals(flag, AttachableResolver.resolveByItemId("demo:flag", truthy));

        MolangScope falsy = new MolangScope();
        falsy.set("variable.flag", 0);
        assertNull(AttachableResolver.resolveByItemId("demo:flag", falsy));
    }

    @Test
    void resolveByItemIdWithScopeReturnsNullWhenConditionItemMissing() {
        BrClientEntity wrench = testAttachable("eyelib:wrench", Map.of("demo:wrench", "1.0"));
        AttachableManager.INSTANCE.put(wrench.identifier(), wrench);

        MolangScope scope = new MolangScope();
        assertNull(AttachableResolver.resolveByItemId("demo:other", scope));
    }

    @Test
    void isAttachableEnabledReturnsTrueWhenHolderCeIsNull() {
        assertTrue(AttachableResolver.isAttachableEnabled(null));
    }

    @Test
    void isAttachableEnabledReturnsTrueWhenEnableAttachablesIsTrue() {
        BrClientEntity ce = testAttachableWithFlag("eyelib:custom", Map.of("demo:item", "1.0"), true);
        assertTrue(AttachableResolver.isAttachableEnabled(ce));
    }

    @Test
    void isAttachableEnabledReturnsFalseWhenEnableAttachablesIsFalse() {
        BrClientEntity ce = testAttachable("eyelib:custom", Map.of("demo:item", "1.0"));
        assertFalse(AttachableResolver.isAttachableEnabled(ce));
    }

    private static BrClientEntity testAttachable(String identifier, Map<String, String> item) {
        return testAttachableWithFlag(identifier, item, false);
    }

    private static BrClientEntity testAttachableWithFlag(String identifier, Map<String, String> item, boolean enableAttachables) {
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
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                item,
                enableAttachables
        );
    }
}
