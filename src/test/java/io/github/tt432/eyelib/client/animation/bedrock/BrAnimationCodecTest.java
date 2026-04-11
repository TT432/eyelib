package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelibimporter.animation.bedrock.BrLoopType;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrAnimationCodecTest {
    @Test
    void parsesRuntimeAnimationFromImporterOwnedSchemaCodec() {
        String json = """
                {
                  \"animations\": {
                    \"animation.test.idle\": {
                      \"loop\": \"true\",
                      \"animation_length\": 1.5,
                      \"timeline\": {
                        \"0.0\": [\"query.life_time\"]
                      },
                      \"bones\": {
                        \"body\": {
                          \"rotation\": {
                            \"0.0\": [0.0, 0.0, 0.0]
                          }
                        }
                      }
                    }
                  }
                }
                """;

        BrAnimation animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        BrAnimationEntry entry = animation.animations().get("animation.test.idle");
        assertNotNull(entry);
        assertEquals("animation.test.idle", entry.name());
        assertEquals(1.5F, entry.animationLength());
        assertEquals(BrLoopType.LOOP, entry.loop());
        assertEquals(1, entry.timeline().data().size());
        assertTrue(entry.bones().size() > 0);
    }
}
