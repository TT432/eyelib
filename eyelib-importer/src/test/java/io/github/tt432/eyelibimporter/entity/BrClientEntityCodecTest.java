package io.github.tt432.eyelibimporter.entity;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

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

        BrClientEntity entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

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

        BrClientEntity entity = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

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

        BrClientEntity entity = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

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

        BrClientEntity entity = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

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

        BrClientEntity entity = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

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

        BrClientEntity entity = BrClientEntity.ATTACHABLE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

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

        BrClientEntity entity = BrClientEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        assertEquals("1.20.80", entity.min_engine_version().orElseThrow().semanticString());
        assertEquals("controller.animation.test", entity.animation_controllers().get(0).get("idle"));
        assertEquals("#ffffff", ((io.github.tt432.eyelibimporter.addon.BedrockResourceValue.StringValue) entity.spawn_egg().orElseThrow().values().get("base_color")).value());
        assertEquals("#000000", ((io.github.tt432.eyelibimporter.addon.BedrockResourceValue.StringValue) entity.spawn_egg().orElseThrow().values().get("overlay_color")).value());
    }
}