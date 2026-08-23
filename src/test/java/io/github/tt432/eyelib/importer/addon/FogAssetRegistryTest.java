package io.github.tt432.eyelib.importer.addon;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FogAssetRegistry} 的 stage 整体替换语义与默认激活规则
 * （恰一个定义自动激活，否则不激活）。
 *
 * @author TT432
 */
class FogAssetRegistryTest {

    private static BrFog fog(String identifier) {
        return new BrFog(identifier, new LinkedHashMap<>(Map.of(
                BrFog.MEDIUM_AIR, new BrFog.DistanceSetting(0.0f, 1.0f, "render", "#FFFFFF"))));
    }

    @Test
    void stageReplacesPreviousContent() {
        FogAssetRegistry.stageFogs(Map.of("pack:a", fog("pack:a")));
        assertTrue(FogAssetRegistry.activeFog().isPresent());

        FogAssetRegistry.stageFogs(Map.of());
        assertTrue(FogAssetRegistry.activeFog().isEmpty());
        assertTrue(FogAssetRegistry.activeFogId().isEmpty());
    }

    @Test
    void singleDefinitionAutoActivates() {
        FogAssetRegistry.stageFogs(Map.of("pack:only", fog("pack:only")));

        assertEquals("pack:only", FogAssetRegistry.activeFogId().orElseThrow());
        assertEquals("pack:only", FogAssetRegistry.activeFog().orElseThrow().identifier());
        FogAssetRegistry.stageFogs(Map.of());
    }

    @Test
    void multipleDefinitionsDoNotAutoActivate() {
        FogAssetRegistry.stageFogs(Map.of("pack:a", fog("pack:a"), "pack:b", fog("pack:b")));

        assertTrue(FogAssetRegistry.activeFog().isEmpty());
        FogAssetRegistry.stageFogs(Map.of());
    }

    @Test
    void explicitActivationAndClear() {
        FogAssetRegistry.stageFogs(Map.of("pack:a", fog("pack:a"), "pack:b", fog("pack:b")));

        FogAssetRegistry.setActiveFog("pack:b");
        assertEquals("pack:b", FogAssetRegistry.activeFogId().orElseThrow());

        FogAssetRegistry.clearActiveFog();
        assertTrue(FogAssetRegistry.activeFog().isEmpty());
        FogAssetRegistry.stageFogs(Map.of());
    }

    @Test
    void activatingUnknownIdThrows() {
        FogAssetRegistry.stageFogs(Map.of("pack:a", fog("pack:a")));

        assertThrows(IllegalArgumentException.class, () -> FogAssetRegistry.setActiveFog("pack:missing"));
        FogAssetRegistry.stageFogs(Map.of());
    }
}
