package io.github.tt432.eyelib.behavior;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.behavior.component.Component;
import io.github.tt432.eyelib.behavior.component.Health;
import io.github.tt432.eyelib.behavior.component.group.ComponentGroup;
import io.github.tt432.eyelib.behavior.component.property.CollisionBox;
import io.github.tt432.eyelib.behavior.component.property.Scale;
import io.github.tt432.eyelib.behavior.component.property.TypeFamily;
import io.github.tt432.eyelib.behavior.event.logic.LogicNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 对照 Mojang Creator 文档 EntityBehaviorIntroduction.md 的规范测试。
 * Oracle 来自 Bedrock 实体 JSON 格式规范。
 *
 * @author TT432
 */
class BehaviorEntitySpecTest {

    /**
     * Mojang 文档 EntityBehaviorIntroduction §Format Overview:
     * "The basic structure looks like this: { format_version, minecraft:entity: { description, components, component_groups, events } }"
     * 验证完整 JSON 结构的 CODEC 往返。
     */
    @Test
    @DisplayName("Mojang §Format Overview: 完整结构 CODEC 往返")
    void fullStructureRoundtrip() {
        var json = """
                {
                    "format_version": "1.19.20",
                    "minecraft:entity": {
                        "description": { "identifier": "test:spec" },
                        "components": {
                            "minecraft:health": { "value": 20, "max": 20 }
                        },
                        "component_groups": {
                            "test:baby": {
                                "minecraft:scale": { "value": 0.5 }
                            }
                        },
                        "events": {
                            "test:grow": {
                                "remove": { "component_groups": ["test:baby"] }
                            }
                        }
                    }
                }
                """;

        var result = BehaviorEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.result().isPresent(), "CODEC 解析应成功: " + result.result());
        var entity = TestCodecUtil.unwrap(result);

        assertEquals("test:spec", entity.identifier().toString(),
                "Mojang 文档: identifier 是命名空间:名称格式");
    }

    /**
     * Mojang 文档: "Components that define active behavior such as visibility, family, health,
     * and collision box behavior. These behaviors are applied to the entity immediately."
     * 验证顶层组件正确解析。
     */
    @Test
    @DisplayName("Mojang §Components: 顶层组件直接生效")
    void topLevelComponentsApplyImmediately() {
        var json = """
                {
                    "format_version": "1.19.20",
                    "minecraft:entity": {
                        "description": { "identifier": "test:comp" },
                        "components": {
                            "minecraft:type_family": { "family": ["mob", "robot"] },
                            "minecraft:collision_box": { "width": 1.0, "height": 2.0 },
                            "minecraft:health": { "value": 15, "max": 15 },
                            "minecraft:scale": { "value": 1.5 }
                        }
                    }
                }
                """;

        var entity = TestCodecUtil.unwrap(BehaviorEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        var components = entity.components().components();
        assertEquals(4, components.size());
        assertInstanceOf(TypeFamily.class, components.get("minecraft:type_family"));
        assertInstanceOf(CollisionBox.class, components.get("minecraft:collision_box"));
        assertInstanceOf(Health.class, components.get("minecraft:health"));
        assertInstanceOf(Scale.class, components.get("minecraft:scale"));
    }

    /**
     * Mojang 文档: "Component groups are not applied but may be evoked by events.
     * They apply additional Minecraft behavior, including behaviors like aging and herd behavior."
     * 验证 component_groups 正确解析（值解析到组件，不作为顶层生效）。
     */
    @Test
    @DisplayName("Mojang §Component groups: 分组存储，不自动激活")
    void componentGroupsAreStoredNotActivated() {
        var json = """
                {
                    "format_version": "1.19.20",
                    "minecraft:entity": {
                        "description": { "identifier": "test:groups" },
                        "components": {
                            "minecraft:health": { "value": 10, "max": 10 }
                        },
                        "component_groups": {
                            "test:baby": {
                                "minecraft:scale": { "value": 0.5 },
                                "minecraft:health": { "value": 5, "max": 5 }
                            },
                            "test:adult": {
                                "minecraft:scale": { "value": 1.0 }
                            }
                        }
                    }
                }
                """;

        var entity = TestCodecUtil.unwrap(BehaviorEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        // 顶层组件只有 health
        assertEquals(1, entity.components().components().size());

        // component_groups 有 2 个
        var groups = entity.component_groups();
        assertEquals(2, groups.size());

        // baby group 包含 scale 和 health
        var babyGroup = groups.get("test:baby");
        assertNotNull(babyGroup);
        Map<String, Component> babyComps = babyGroup.components();
        assertTrue(babyComps.containsKey("minecraft:scale"),
                "Mojang 文档: component_groups 中可以包含组件定义");
        assertTrue(babyComps.containsKey("minecraft:health"));
    }

    /**
     * Mojang 文档 EntityEvents: events 支持 add/remove/trigger 响应类型。
     * 验证 events 解析正确（非空 Map）。
     */
    @Test
    @DisplayName("Mojang §Events: add/remove/trigger 事件类型解析")
    void eventsParseCorrectly() {
        var json = """
                {
                    "format_version": "1.19.20",
                    "minecraft:entity": {
                        "description": { "identifier": "test:events" },
                        "components": {},
                        "component_groups": {
                            "test:baby": { "minecraft:scale": { "value": 0.5 } },
                            "test:hostile": { "minecraft:health": { "value": 30, "max": 30 } }
                        },
                        "events": {
                            "test:set_baby": {
                                "add": { "component_groups": ["test:baby"] }
                            },
                            "test:grow_up": {
                                "remove": { "component_groups": ["test:baby"] },
                                "trigger": { "event": "test:become_hostile", "target": "self" }
                            }
                        }
                    }
                }
                """;

        var entity = TestCodecUtil.unwrap(BehaviorEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        var events = entity.events();
        // Mojang 文档: 2 个事件
        assertEquals(2, events.size(),
                "Mojang 文档: 应解析出 2 个事件（set_baby + grow_up）");

        assertTrue(events.containsKey("test:set_baby"),
                "events 应包含 test:set_baby");
        assertTrue(events.containsKey("test:grow_up"),
                "events 应包含 test:grow_up");

        var babyEvent = events.get("test:set_baby");
        assertNotNull(babyEvent);
        assertInstanceOf(LogicNode.class, babyEvent,
                "事件应被解析为 LogicNode");
    }

    /**
     * 验证可选字段缺失时使用默认值（空 Map）。
     */
    @Test
    @DisplayName("可选字段: 缺失 component_groups 和 events 应默认为空")
    void missingOptionalFieldsDefaultToEmpty() {
        var json = """
                {
                    "format_version": "1.19.20",
                    "minecraft:entity": {
                        "description": { "identifier": "test:minimal" },
                        "components": { "minecraft:health": { "value": 5 } }
                    }
                }
                """;

        var entity = TestCodecUtil.unwrap(BehaviorEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertTrue(entity.component_groups().isEmpty(),
                "未定义 component_groups 时应为空 Map");
        assertTrue(entity.events().isEmpty(),
                "未定义 events 时应为空 Map");
    }
}
