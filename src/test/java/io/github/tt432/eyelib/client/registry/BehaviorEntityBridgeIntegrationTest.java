package io.github.tt432.eyelib.client.registry;

import com.google.gson.JsonParser;
import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelib.behavior.BehaviorEntity;
import io.github.tt432.eyelib.behavior.component.Health;
import io.github.tt432.eyelib.behavior.component.RawComponent;
import io.github.tt432.eyelib.behavior.component.Variant;
import io.github.tt432.eyelib.importer.addon.BrBehaviorEntityFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BehaviorEntity bridge 层的集成测试。
 * 验证从 JSON 解析 → BrBehaviorEntityFile → BehaviorEntity 的完整链路。
 *
 * @author TT432
 */
class BehaviorEntityBridgeIntegrationTest {

    @BeforeEach
    void setUp() {
        BehaviorEntityManager.INSTANCE.clear();
    }

    @Test
    void fullParsePipelineWithComponents() {
        var json = """
                {
                    "format_version": "1.20.0",
                    "minecraft:entity": {
                        "description": { "identifier": "test:health_entity" },
                        "component_groups": {},
                        "components": {
                            "minecraft:health": { "value": 30, "max": 30 },
                            "minecraft:variant": { "value": 2 },
                            "minecraft:unknown_test": { "some_field": true }
                        },
                        "events": {}
                    }
                }
                """;

        // 1. importer 解析
        BrBehaviorEntityFile file = BrBehaviorEntityFile.parse(JsonParser.parseString(json).getAsJsonObject());
        assertNotNull(file.components());
        assertNotNull(file.componentGroups());
        assertEquals("test:health_entity", file.identifier());

        // 2. bridge 转换（通过 public API）
        Map<String, BrBehaviorEntityFile> input = new LinkedHashMap<>();
        input.put(file.identifier(), file);
        BehaviorEntityAssetRegistry.replaceBehaviorEntities(input);

        // 3. 验证实体已加载到 manager
        BehaviorEntity entity = BehaviorEntityManager.INSTANCE.get("test:health_entity");
        assertNotNull(entity, "Entity should be loaded into BehaviorEntityManager");
        assertNotNull(entity.components(), "BehaviorEntity should have components field");

        // 4. 验证 components
        assertFalse(entity.components().components().isEmpty(), "Should have parsed top-level components");

        // 4a. minecraft:health → Health typed component
        var health = entity.components().components().get("minecraft:health");
        assertNotNull(health, "health component should be present");
        assertInstanceOf(Health.class, health);
        assertEquals(30, ((Health) health).value());
        assertEquals(30, ((Health) health).max());

        // 4b. minecraft:variant → Variant typed component
        var variant = entity.components().components().get("minecraft:variant");
        assertNotNull(variant, "variant component should be present");
        assertInstanceOf(Variant.class, variant);
        assertEquals(2, ((Variant) variant).value());

        // 4c. 未知组件保留为 RawComponent
        var unknown = entity.components().components().get("minecraft:unknown_test");
        assertNotNull(unknown, "unknown component should be preserved as RawComponent");
        assertInstanceOf(RawComponent.class, unknown);
        assertEquals("minecraft:unknown_test", ((RawComponent) unknown).componentId());
        assertNotNull(((RawComponent) unknown).rawData());
        assertTrue(((RawComponent) unknown).rawData().get("some_field").getAsBoolean());
    }

    @Test
    void entityWithoutComponents() {
        var json = """
                {
                    "format_version": "1.20.0",
                    "minecraft:entity": {
                        "description": { "identifier": "test:simple_entity" },
                        "component_groups": {},
                        "components": {},
                        "events": {}
                    }
                }
                """;

        BrBehaviorEntityFile file = BrBehaviorEntityFile.parse(JsonParser.parseString(json).getAsJsonObject());
        Map<String, BrBehaviorEntityFile> input = new LinkedHashMap<>();
        input.put(file.identifier(), file);
        BehaviorEntityAssetRegistry.replaceBehaviorEntities(input);

        BehaviorEntity entity = BehaviorEntityManager.INSTANCE.get("test:simple_entity");
        assertNotNull(entity);
        assertNotNull(entity.components());
        assertTrue(entity.components().components().isEmpty());
    }
}
