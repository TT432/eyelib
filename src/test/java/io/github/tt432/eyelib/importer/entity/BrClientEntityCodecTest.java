package io.github.tt432.eyelib.importer.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class BrClientEntityCodecTest {
    @Test
    void parsesClientEntitySchemaFromImporterOwnedCodec() {
        String json = """
                {
                  \"minecraft:client_entity\": {
                    \"description\": {
                      \"identifier\": \"eyelib:test_entity\",
                      \"geometry\": { \"default\": \"geometry.test\" },
                      \"textures\": { \"default\": \"textures/test\" },
                      \"animations\": { \"idle\": \"animation.test.idle\" },
                      \"render_controllers\": [\"controller.render.test\"]
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertEquals("eyelib:test_entity", entity.identifier());
        assertEquals("geometry.test", entity.geometry().get("default"));
        assertEquals("textures/test.png", entity.textures().get("default"));
        assertEquals("animation.test.idle", entity.animations().get("idle"));
        assertTrue(entity.min_engine_version().isEmpty());
        assertTrue(entity.animation_controllers().isEmpty());
        assertTrue(entity.spawn_egg().isEmpty());
        assertTrue(entity.scripts().isEmpty());
    }

    @Test
    void parsesAttachableSchemaFromSharedImporterCodec() {
        String json = """
                {
                  \"minecraft:attachable\": {
                    \"description\": {
                      \"identifier\": \"eyelib:test_attachable\",
                      \"geometry\": { \"default\": \"geometry.attachable\" },
                      \"textures\": { \"default\": \"textures/attachable\" },
                      \"scripts\": {
                        \"scale\": \"1.0\",
                        \"animate\": [\"animation.attachable.idle\"]
                      }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertEquals("eyelib:test_attachable", entity.identifier());
        assertEquals("geometry.attachable", entity.geometry().get("default"));
        assertEquals("textures/attachable.png", entity.textures().get("default"));
        assertEquals(1, entity.scripts().orElseThrow().animate().size());
        assertTrue(entity.scripts().orElseThrow().animate().containsKey("animation.attachable.idle"));
        assertTrue(entity.item().isEmpty());
        assertFalse(entity.enable_attachables());
    }

    @Test
    void parsesAttachableItemAsSimpleString() {
        String json = """
                {
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "eyelib:test_attachable",
                      "item": "minecraft:stick",
                      "geometry": { "default": "geometry.attachable" }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertEquals(1, entity.item().size());
        assertTrue(entity.item().containsKey("minecraft:stick"));
        assertEquals("1.0", entity.item().get("minecraft:stick"));
    }

    @Test
    void parsesAttachableItemAsConditionalObject() {
        String json = """
                {
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "eyelib:test_attachable",
                      "item": {
                        "minecraft:stick": "query.is_owner_identifier_any('minecraft:player')"
                      },
                      "geometry": { "default": "geometry.attachable" }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertEquals(1, entity.item().size());
        assertTrue(entity.item().containsKey("minecraft:stick"));
        assertEquals("query.is_owner_identifier_any('minecraft:player')", entity.item().get("minecraft:stick"));
    }

    @Test
    void parsesAttachableWithEnableAttachablesFlag() {
        String json = """
                {
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "eyelib:test_attachable",
                      "enable_attachables": true,
                      "geometry": { "default": "geometry.attachable" }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertTrue(entity.enable_attachables());
    }

    @Test
    void parsesAttachableWithParentSetupScript() {
        String json = """
                {
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "eyelib:test_attachable",
                      "geometry": { "default": "geometry.attachable" },
                      "scripts": {
                        "parent_setup": "variable.chest_layer_visible = 0.0;"
                      }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertTrue(entity.scripts().isPresent());
        assertEquals("variable.chest_layer_visible = 0.0;", entity.scripts().orElseThrow().parent_setup().toString());
    }

    @Test
    void parsesDocumentedClientEntityFieldsFromImporterOwnedCodec() {
        String json = """
                {
                  \"minecraft:client_entity\": {
                    \"description\": {
                      \"identifier\": \"eyelib:test_entity\",
                      \"min_engine_version\": \"1.20.80\",
                      \"animation_controllers\": [
                        { \"idle\": \"controller.animation.test\" }
                      ],
                      \"spawn_egg\": {
                        \"base_color\": \"#ffffff\",
                        \"overlay_color\": \"#000000\"
                      }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertEquals("1.20.80", entity.min_engine_version().orElseThrow().semanticString());
        assertEquals("controller.animation.test", entity.animation_controllers().get(0).get("idle"));
        assertEquals("#ffffff", ((io.github.tt432.eyelib.importer.addon.BedrockResourceValue.StringValue) entity.spawn_egg().orElseThrow().values().get("base_color")).value());
        assertEquals("#000000", ((io.github.tt432.eyelib.importer.addon.BedrockResourceValue.StringValue) entity.spawn_egg().orElseThrow().values().get("overlay_color")).value());
    }

    @Test
    void inlineRenderControllerConditionsAreCaptured() {
        String json = """
                {
                  "minecraft:attachable": {
                    "description": {
                      "identifier": "eyelib:test_attachable",
                      "render_controllers": [
                        { "controller.render.conditional": "query.is_enchanted" },
                        "controller.render.always"
                      ]
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertEquals(java.util.List.of("controller.render.conditional", "controller.render.always"),
                entity.render_controllers());
        assertEquals(1, entity.renderControllerConditions().size());
        assertTrue(entity.renderControllerConditions().containsKey("controller.render.conditional"));
    }

    @Test
    void explicitRenderControllerConditionsWinOverInline() {
        String json = """
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "eyelib:test_entity",
                      "render_controllers": [
                        { "controller.render.a": "1.0" }
                      ],
                      "render_controller_conditions": {
                        "controller.render.a": "0.0"
                      }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertEquals(java.util.List.of("controller.render.a"), entity.render_controllers());
        assertEquals(1, entity.renderControllerConditions().size());
        assertFalse(entity.renderControllerConditions().get("controller.render.a")
                .evalAsBool(new io.github.tt432.eyelib.molang.MolangScope()));
    }

    @Test
    void recordConstructorPreservesMapIterationOrder() {
        // spec §8.9「零编辑往返逐字节一致」：Map.copyOf 的迭代序是输入 map 的哈希展开序，
        // CODEC-decode（HashMap 序）与 draft（LinkedHashMap 序）两条来源会分歧。
        // record 构造的防御性拷贝必须保序，否则 encode(draft) 与 encode(原实体) 文本不一致。
        LinkedHashMap<String, String> particleEffects = new LinkedHashMap<>();
        LinkedHashMap<String, String> soundEffects = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            particleEffects.put("eff" + (char) ('a' + i) + i, "ns:particle." + i);
            soundEffects.put("snd" + (char) ('a' + i) + i, "sound." + i);
        }

        BrClientEntity entity = new BrClientEntity(
                "eyelib:order",
                Optional.empty(),
                Map.of(), Map.of(), Map.of(), Map.of(),
                List.of(),
                particleEffects,
                soundEffects,
                List.of(),
                Map.of(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                BrClientEntity.Toggles.DEFAULT);

        assertEquals(particleEffects.keySet().stream().toList(),
                entity.particle_effects().keySet().stream().toList());
        assertEquals(soundEffects.keySet().stream().toList(),
                entity.sound_effects().keySet().stream().toList());

        // 文本级往返：encode(decode(x)) 与 encode(原实体) 逐字节一致
        BrClientEntity decoded = TestCodecUtil.unwrap(
                BrClientEntity.CODEC.parse(JsonOps.INSTANCE,
                        TestCodecUtil.unwrap(BrClientEntity.CODEC.encodeStart(JsonOps.INSTANCE, entity))));
        assertEquals(entity, decoded);
    }

    @Test
    void parsesEquipmentRenderToggles() {
        String json = """
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "eyelib:test_toggles",
                      "geometry": { "default": "geometry.test" },
                      "enable_attachables": true,
                      "hide_armor": true,
                      "held_item_ignores_lighting": true,
                      "held_item_scale": "2.0"
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(
                BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertTrue(entity.enable_attachables());
        assertTrue(entity.hide_armor());
        assertTrue(entity.held_item_ignores_lighting());
        assertEquals(2.0F, entity.held_item_scale().eval(new io.github.tt432.eyelib.molang.MolangScope()), 1e-6F);
        // 四个开关透传给渲染策略，不改变其它字段
        assertEquals("geometry.test", entity.geometry().get("default"));
    }

    @Test
    void equipmentRenderTogglesDefaultWhenAbsent() {
        String json = """
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "eyelib:test_toggles_default",
                      "geometry": { "default": "geometry.test" }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(
                BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));

        assertFalse(entity.enable_attachables());
        assertFalse(entity.hide_armor());
        assertFalse(entity.held_item_ignores_lighting());
        assertEquals(1.0F, entity.held_item_scale().eval(new io.github.tt432.eyelib.molang.MolangScope()), 1e-6F);
    }

    @Test
    void absentEquipmentRenderTogglesStayAbsentAfterEncode() {
        String json = """
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "eyelib:test_toggles_roundtrip",
                      "geometry": { "default": "geometry.test" }
                    }
                  }
                }
                """;

        BrClientEntity entity = TestCodecUtil.unwrap(
                BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        JsonElement encoded = TestCodecUtil.unwrap(BrClientEntity.CODEC.encodeStart(JsonOps.INSTANCE, entity));
        JsonObject description = encoded.getAsJsonObject().getAsJsonObject("minecraft:client_entity")
                                        .getAsJsonObject("description");

        // 导出不得凭空写入缺省开关（否则整包资源往返会污染所有 client entity JSON）
        assertFalse(description.has("enable_attachables"), description.toString());
        assertFalse(description.has("hide_armor"), description.toString());
        assertFalse(description.has("held_item_ignores_lighting"), description.toString());
        assertFalse(description.has("held_item_scale"), description.toString());
    }
}
