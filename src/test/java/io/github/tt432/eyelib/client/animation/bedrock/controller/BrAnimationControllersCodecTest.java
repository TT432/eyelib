package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibimporter.animation.NamedTrackContainerDefinition;
import io.github.tt432.eyelib.client.animation.StateMachineAnimation;
import io.github.tt432.eyelibimporter.animation.StateMachineAnimationDefinition;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrAnimationControllersCodecTest {
    @Test
    void parsesRuntimeControllersFromImporterOwnedSchemaCodec() {
        String json = """
                {
                  \"animation_controllers\": {
                    \"controller.animation.test\": {
                      \"initial_state\": \"default\",
                      \"states\": {
                        \"default\": {
                          \"animations\": [\"animation.test.idle\"]
                        }
                      }
                    }
                  }
                }
                """;

        BrAnimationControllers controllers = BrAnimationControllers.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow(false, message -> {
                    throw new AssertionError(message);
                });

        BrAnimationController controller = controllers.animationControllers().get("controller.animation.test");
        assertNotNull(controller);
        assertEquals("controller.animation.test", controller.name());
        assertEquals(1, controller.states().size());
        assertTrue(controller.states().containsKey("default"));
        assertEquals(controller.states().get("default"), controller.initialState());
        assertTrue(controller.initialState().animations().containsKey("animation.test.idle"));

        NamedTrackContainerDefinition<io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackName, io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackDefinition> tracks = controller.initialState().namedTracks();
        assertEquals(controller.initialState().namedTracks().tracksByName(), controller.initialState().namedTracks().byName());
        assertEquals(controller.initialState().animations(), ((io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateAnimationsTrackDefinition) tracks.trackOrNull(io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackName.ANIMATIONS)).animations());

        StateMachineAnimationDefinition<BrAcStateDefinition> definition = controller.definition();
        assertEquals(controller.initialState(), definition.initialState());
        assertEquals(controller.states().get("default"), definition.state("default"));

        StateMachineAnimation<BrAnimationController.Data, BrAcStateDefinition> animation = controller;
        assertEquals(controller.initialState(), animation.initialState());
        assertEquals(controller.states().get("default"), animation.state("default"));
    }
}
