package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.AnimationLookup;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrAnimationControllerBehaviorTest {
    @AfterEach
    void tearDown() {
        AnimationManager.writePort().clear();
    }

    @Test
    void fromSchemaFallsBackToDefaultStateWhenInitialStateIsMissing() {
        BrAcState defaultState = new BrAcState(
                Map.of("slot.main", MolangValue.ONE),
                MolangValue.ZERO,
                MolangValue.ZERO,
                List.of(),
                List.of(),
                Map.of(),
                0F,
                false
        );

        BrAnimationController controller = BrAnimationController.fromSchema("controller.animation.test",
                new BrAnimationControllerSchema("missing", Map.of("default", defaultState)));

        assertEquals(defaultState.animations(), controller.initialState().animations());
    }

    @Test
    void allAnimationFinishedCurrentlyUsesChildAnyAnimationFinishedSemantics() {
        TestAnimation child = new TestAnimation("animation.test.idle", true, false);
        AnimationManager.writePort().put(child.name(), child);

        BrAcState state = new BrAcState(
                Map.of("slot.main", MolangValue.ONE),
                MolangValue.ZERO,
                MolangValue.ZERO,
                List.of(),
                List.of(),
                Map.of(),
                0F,
                false
        );
        BrAnimationController controller = new BrAnimationController("controller.animation.test", state, Map.of("default", state));
        BrAnimationController.Data data = controller.createData();
        controller.tickAnimation(data, Map.of("slot.main", child.name()), new MolangScope(),
                2F, 1F, new ModelRuntimeData(), new AnimationEffects(), () -> {
                });

        assertTrue(controller.anyAnimationFinished(data));
        assertTrue(controller.allAnimationFinished(data));
        assertEquals(2, child.finishedChecks);
    }

    private static final class TestAnimation implements Animation<Object> {
        private final String name;
        private final boolean anyFinished;
        @SuppressWarnings("unused")
        private final boolean allFinished;
        private int finishedChecks;

        private TestAnimation(String name, boolean anyFinished, boolean allFinished) {
            this.name = name;
            this.anyFinished = anyFinished;
            this.allFinished = allFinished;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void onFinish(Object data) {
        }

        @Override
        public boolean anyAnimationFinished(Object data) {
            finishedChecks++;
            return anyFinished;
        }

        @Override
        public boolean allAnimationFinished(Object data) {
            throw new AssertionError("Controller currently delegates to child anyAnimationFinished().");
        }

        @Override
        public Object createData() {
            return new Object();
        }

        @Override
        public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                                  ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback) {
        }
    }
}
