package io.github.tt432.eyelib.importer.addon;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UiAssetRegistry}：整体替换语义、{@link UiAssetRegistry#version()} 每次 stage 自增、
 * stage 时生成 {@code @} 引用缺失 / 未知 key 形态诊断。
 *
 * <p>注册表为全局静态状态：本类只断言「本次 stage 后的相对结果」，不依赖初始值。
 *
 * @author TT432
 */
class UiAssetRegistryTest {

    private static BrUiFile ui(String json) {
        return BrUiFile.parse(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void stageReplacesWholesale() {
        UiAssetRegistry.stageUiFiles(Map.of(
                "ui/a.json", ui("{ \"namespace\": \"a\", \"root\": {} }")));

        UiAssetRegistry.stageUiFiles(Map.of(
                "ui/b.json", ui("{ \"namespace\": \"b\", \"root\": {} }")));

        assertEquals(java.util.Set.of("ui/b.json"), UiAssetRegistry.filesView().keySet());
    }

    @Test
    void versionIncrementsPerStage() {
        long before = UiAssetRegistry.version();
        UiAssetRegistry.stageUiFiles(Map.of(
                "ui/v.json", ui("{ \"namespace\": \"v\", \"root\": {} }")));
        long afterFirst = UiAssetRegistry.version();
        UiAssetRegistry.stageUiFiles(Map.of());

        assertTrue(afterFirst > before);
        assertEquals(afterFirst + 1, UiAssetRegistry.version());
    }

    @Test
    void missingInheritanceTargetProducesDiagnostic() {
        UiAssetRegistry.stageUiFiles(Map.of(
                "ui/hud.json", ui("""
                        { "namespace": "hud", "my_button@common.button": {} }
                        """)));

        assertTrue(UiAssetRegistry.diagnostics().stream()
                .anyMatch(d -> d.contains("common") && d.contains("my_button@common.button")));
    }

    @Test
    void resolvableInheritanceProducesNoDiagnostic() {
        UiAssetRegistry.stageUiFiles(Map.of(
                "ui/common.json", ui("""
                        { "namespace": "common", "button": { "type": "panel" } }
                        """),
                "ui/hud.json", ui("""
                        { "namespace": "hud", "my_button@common.button": {} }
                        """)));

        assertTrue(UiAssetRegistry.diagnostics().isEmpty());
    }

    @Test
    void unknownKeyShapeProducesDiagnostic() {
        UiAssetRegistry.stageUiFiles(Map.of(
                "ui/weird.json", ui("""
                        { "namespace": "weird", "@dangling": {}, "double@a.b@c.d": {} }
                        """)));

        assertTrue(UiAssetRegistry.diagnostics().size() >= 2);
        assertTrue(UiAssetRegistry.diagnostics().stream().anyMatch(d -> d.contains("@dangling")));
        assertTrue(UiAssetRegistry.diagnostics().stream().anyMatch(d -> d.contains("double@a.b@c.d")));
    }

    @Test
    void sameFileInheritanceWithoutNamespaceIsLenient() {
        UiAssetRegistry.stageUiFiles(Map.of(
                "ui/local.json", ui("""
                        {
                          "namespace": "local",
                          "base": { "type": "panel" },
                          "derived@base": {}
                        }
                        """)));

        assertTrue(UiAssetRegistry.diagnostics().isEmpty());
    }
}
