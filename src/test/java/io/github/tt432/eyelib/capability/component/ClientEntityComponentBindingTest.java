package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import io.github.tt432.eyelib.util.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 绑定语义二分（G5）：按 id 自动解析参与代际失效，固定实例永不参与。
 *
 * @author TT432
 */
class ClientEntityComponentBindingTest {

    @Test
    void unresolvedComponentNeedsRebind() {
        ClientEntityComponent component = new ClientEntityComponent();

        assertEquals(ClientEntityComponent.BindingKind.AUTO_BY_ID, component.bindingKind());
        assertNull(component.boundId());
        assertTrue(component.needsRebind(0));
    }

    @Test
    void applyResolvedRecordsGenerationEvenForNullResult() {
        ClientEntityComponent component = new ClientEntityComponent();
        component.bindById("minecraft:test");

        component.applyResolved(null, 3);
        // 解析结果为 null（vanilla 实体）同样记录代际，否则每帧重复查表
        assertFalse(component.needsRebind(3));
        assertTrue(component.needsRebind(4));
        assertNull(component.getClientEntity());
    }

    @Test
    void registryMutationInvalidatesResolvedGeneration() {
        Registry<BrClientEntity> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        BrClientEntity entity = testEntity("eyelib:test_entity");
        registry.put(entity.identifier(), entity);

        ClientEntityComponent component = new ClientEntityComponent();
        component.bindById(entity.identifier());
        component.applyResolved(registry.get(entity.identifier()), registry.generation());
        assertFalse(component.needsRebind(registry.generation()));

        // 模拟 addon 卸载：replaceAll 空视图后旧解析结果必须过期
        registry.replaceAll(Map.of());
        assertTrue(component.needsRebind(registry.generation()));
        assertNull(registry.get(entity.identifier()));
    }

    @Test
    void instanceBindingNeverRebindsAndIgnoresResolvedResults() {
        ClientEntityComponent component = new ClientEntityComponent();
        BrClientEntity explicit = testEntity("eyelib:explicit");
        BrClientEntity resolved = testEntity("eyelib:resolved");

        component.bindInstance(explicit);
        assertEquals(ClientEntityComponent.BindingKind.INSTANCE, component.bindingKind());
        assertNull(component.boundId());
        assertFalse(component.needsRebind(0));
        assertFalse(component.needsRebind(999));

        // 未请求的解析结果不得覆盖显式实例
        component.applyResolved(resolved, 42);
        assertSame(explicit, component.getClientEntity());
        assertFalse(component.needsRebind(43));
    }

    @Test
    void instanceBindingSameReferenceIsIdempotent() {
        ClientEntityComponent component = new ClientEntityComponent();
        BrClientEntity explicit = testEntity("eyelib:explicit");

        component.bindInstance(explicit);
        assertTrue(component.consumeChanged());
        component.bindInstance(explicit);
        // 同一引用不 bump 版本：逐帧重设实例的调用方靠这条避免 RC 每帧重建
        assertFalse(component.consumeChanged());
    }

    @Test
    void bindByIdKeepsCurrentInstanceToAvoidVanillaFlash() {
        ClientEntityComponent component = new ClientEntityComponent();
        BrClientEntity explicit = testEntity("eyelib:explicit");
        component.bindInstance(explicit);

        component.bindById("eyelib:explicit");

        assertEquals(ClientEntityComponent.BindingKind.AUTO_BY_ID, component.bindingKind());
        assertEquals("eyelib:explicit", component.boundId());
        // 写 id 与解析之间不清空实例：否则会闪一帧原版渲染
        assertSame(explicit, component.getClientEntity());
        assertTrue(component.needsRebind(7));
    }

    @Test
    void bindByIdReplacesInstanceAfterResolution() {
        ClientEntityComponent component = new ClientEntityComponent();
        BrClientEntity before = testEntity("eyelib:a");
        BrClientEntity after = testEntity("eyelib:a");
        component.bindInstance(before);
        component.bindById("eyelib:a");

        component.applyResolved(after, 1);
        assertNotSame(before, component.getClientEntity());
        assertSame(after, component.getClientEntity());
        assertFalse(component.needsRebind(1));
    }

    @Test
    void explicitNullInstanceFreezesComponent() {
        ClientEntityComponent component = new ClientEntityComponent();
        component.bindInstance(null);

        assertEquals(ClientEntityComponent.BindingKind.INSTANCE, component.bindingKind());
        assertNull(component.getClientEntity());
        assertFalse(component.needsRebind(5));
        // 显式「无模型」不等于「保持现状」：解析结果不得让它复活
        component.applyResolved(testEntity("eyelib:x"), 6);
        assertNull(component.getClientEntity());
    }

    private static BrClientEntity testEntity(String identifier) {
        return new BrClientEntity(
                identifier,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Optional.empty()
        );
    }
}
