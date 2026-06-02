package io.github.tt432.eyelib.client.registry;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibanimation.AnimationLookup;
import io.github.tt432.eyelibanimation.bedrock.BrAnimation;
import io.github.tt432.eyelibanimation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelibanimation.AnimationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** @author TT432 */
class ClientAssetRegistryTest {
    @AfterEach
    void tearDown() {
        AnimationManager.INSTANCE.clear();
    }

    @Test
    void replaceAnimationAssetsKeepsAnimationsAndControllersTogether() {
        BrAnimation animation = BrAnimation.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
                {
                  "animations": {
                    "animation.test.idle": {}
                  }
                }
                """)).getOrThrow(false, AssertionError::new);
        BrAnimationControllers controllers = BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("""
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
                """)).getOrThrow(false, AssertionError::new);

        AnimationAssetRegistry.stageAnimations(Map.of("animations", animation));
        AnimationAssetRegistry.stageControllers(Map.of("controllers", controllers));

        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(AnimationLookup.get("controller.animation.test"));
        assertEquals(2, AnimationLookup.size());
    }
}
