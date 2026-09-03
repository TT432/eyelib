package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import io.github.tt432.eyelib.util.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class ClientEntityComponentStalenessTest {
    @Test
    void unresolvedComponentIsStale() {
        ClientEntityComponent component = new ClientEntityComponent();

        assertTrue(component.isStale(0));
    }

    @Test
    void markedComponentIsFreshAtSameGenerationAndStaleAfterBump() {
        ClientEntityComponent component = new ClientEntityComponent();
        // 解析结果为 null（vanilla 实体）也必须记录代际，否则每帧重复查表
        component.setClientEntity(null);
        component.markResolvedFrom(3);

        assertFalse(component.isStale(3));
        assertTrue(component.isStale(4));
    }

    @Test
    void registryMutationInvalidatesRecordedGeneration() {
        Registry<BrClientEntity> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        BrClientEntity entity = testEntity();
        registry.put(entity.identifier(), entity);

        ClientEntityComponent component = new ClientEntityComponent();
        component.setClientEntity(registry.get(entity.identifier()));
        component.markResolvedFrom(registry.generation());
        assertFalse(component.isStale(registry.generation()));

        // 模拟 addon 卸载：replaceAll 空视图后旧解析结果必须过期
        registry.replaceAll(Map.of());
        assertTrue(component.isStale(registry.generation()));
        assertNull(registry.get(entity.identifier()));
    }
    @Test
    void externalSetIsNotInvalidatedByGenerationBump() {
        Registry<BrClientEntity> registry = new Registry<>("TestRegistry", ManagerEventPublisher.NOOP);
        BrClientEntity entity = testEntity();
        registry.put(entity.identifier(), entity);

        ClientEntityComponent component = new ClientEntityComponent();
        // 先走一遍 id 解析路径（记录代际），再被外部显式覆盖——
        // 外部设置必须清除代际失效语义，否则资源重载会把外部模型擦掉
        component.setClientEntity(registry.get(entity.identifier()));
        component.markResolvedFrom(registry.generation());

        component.setClientEntity(entity);
        assertFalse(component.isStale(registry.generation()));

        registry.replaceAll(Map.of());
        assertFalse(component.isStale(registry.generation()));
    }

    @Test
    void markResolvedFromAfterExternalSetRestoresInvalidation() {
        ClientEntityComponent component = new ClientEntityComponent();
        component.setClientEntity(testEntity());
        assertFalse(component.isStale(7));

        // id 解析路径重新接管后恢复代际失效语义
        component.markResolvedFrom(7);
        assertFalse(component.isStale(7));
        assertTrue(component.isStale(8));
    }

    private static BrClientEntity testEntity() {
        return new BrClientEntity(
                "eyelib:test_entity",
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
