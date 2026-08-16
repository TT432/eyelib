package io.github.tt432.eyelib.client.loader;

import io.github.tt432.eyelib.util.manager.ManagerEventPublisher;
import io.github.tt432.eyelib.util.registry.Registry;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link BedrockAddonRuntimeBridge} 的 addon 叠加阴影语义：包禁用/移除后注册表
 * 恢复基线，mod 自带条目不被吞。
 *
 * @author TT432
 */
class BedrockAddonOverlayTest {

    @Test
    void overlayThenEmptyRoundRestoresBaseAndDropsAddonOnlyKeys() {
        Registry<String> registry = new Registry<>("Test", ManagerEventPublisher.NOOP);
        registry.put("modKey", "modValue");

        // 第一轮：addon 覆盖 modKey、新增 addonKey
        Map<String, String> round1 = Map.of("modKey", "addonValue1", "addonKey", "addonOnly");
        BedrockAddonRuntimeBridge.beginOverlay(registry, round1.keySet());
        BedrockAddonRuntimeBridge.applyOverlay(registry, round1);
        assertEquals("addonValue1", registry.get("modKey"));
        assertEquals("addonOnly", registry.get("addonKey"));

        // 第二轮：无选中包 → modKey 恢复、addonKey 移除
        BedrockAddonRuntimeBridge.beginOverlay(registry, Set.of());
        BedrockAddonRuntimeBridge.applyOverlay(registry, Map.of());
        assertEquals("modValue", registry.get("modKey"));
        assertNull(registry.get("addonKey"));
    }

    @Test
    void persistingKeyKeepsOriginalShadowAcrossRounds() {
        Registry<String> registry = new Registry<>("Test", ManagerEventPublisher.NOOP);
        registry.put("modKey", "modValue");

        Map<String, String> round1 = Map.of("modKey", "addonValue1");
        BedrockAddonRuntimeBridge.beginOverlay(registry, round1.keySet());
        BedrockAddonRuntimeBridge.applyOverlay(registry, round1);

        // 第二轮仍提供 modKey（新值）：阴影不得被 addon 旧值污染
        Map<String, String> round2 = Map.of("modKey", "addonValue2");
        BedrockAddonRuntimeBridge.beginOverlay(registry, round2.keySet());
        BedrockAddonRuntimeBridge.applyOverlay(registry, round2);
        assertEquals("addonValue2", registry.get("modKey"));

        // 第三轮清空：恢复的是 mod 原值而不是 addonValue1
        BedrockAddonRuntimeBridge.beginOverlay(registry, Set.of());
        BedrockAddonRuntimeBridge.applyOverlay(registry, Map.of());
        assertEquals("modValue", registry.get("modKey"));
    }

    @Test
    void untouchedKeysAreNeverDisturbed() {
        Registry<String> registry = new Registry<>("Test", ManagerEventPublisher.NOOP);
        registry.put("untouched", "base");

        Map<String, String> round1 = Map.of("addonKey", "addonOnly");
        BedrockAddonRuntimeBridge.beginOverlay(registry, round1.keySet());
        BedrockAddonRuntimeBridge.applyOverlay(registry, round1);
        BedrockAddonRuntimeBridge.beginOverlay(registry, Set.of());
        BedrockAddonRuntimeBridge.applyOverlay(registry, Map.of());

        assertEquals("base", registry.get("untouched"));
        assertEquals(1, registry.all().size());
    }
}
