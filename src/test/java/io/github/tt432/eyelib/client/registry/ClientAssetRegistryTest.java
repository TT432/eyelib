package io.github.tt432.eyelib.client.registry;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.AnimationRegistries;
import io.github.tt432.eyelib.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.animation.bedrock.controller.BrAnimationControllers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** @author TT432 */
class ClientAssetRegistryTest {
    @AfterEach
    void tearDown() {
        AnimationRegistries.animation().clear();
    }

    @Test
    void replaceAnimationAssetsKeepsAnimationsAndControllersTogether() {
        BrAnimation animation = TestCodecUtil.unwrap(BrAnimation.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "animations": {
                    "animation.test.idle": {}
                  }
                }
                """)));
        BrAnimationControllers controllers = TestCodecUtil.unwrap(BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "animation_controllers": {
                    "controller.animation.test": {
                      "initial_state": "default",
                      "states": {
                        "default": {
                          "animations": ["animation.test.idle"]
                        }
                      }
                    }
                  }
                }
                """)));

        AnimationAssetRegistry.stageAnimations(Map.of("animations", animation));
        AnimationAssetRegistry.stageControllers(Map.of("controllers", controllers));

        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(AnimationLookup.get("controller.animation.test"));
        assertEquals(2, AnimationLookup.size());
    }
}
