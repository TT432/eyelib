package io.github.tt432.eyelibimporter.entity;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    }
}
