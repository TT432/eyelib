package io.github.tt432.eyelib.client.registry;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;
import io.github.tt432.eyelib.client.animation.bedrock.controller.BrAnimationControllers;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        ClientAssetRegistry.replaceAnimationAssets(Map.of("animations", animation), Map.of("controllers", controllers));

        assertNotNull(AnimationLookup.get("animation.test.idle"));
        assertNotNull(AnimationLookup.get("controller.animation.test"));
    }
}
