package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BrAnimationControllerStateOwnerTest {
    @AfterEach
    void tearDown() {
        AnimationManager.writePort().clear();
    }

    @Test
    void tickAnimationInitializesOwnerBackedControllerStateAndCachesChildStateByAnimationName() {
        AtomicInteger createdStates = new AtomicInteger();
        TestAnimation child = new TestAnimation("animation.test.child", createdStates);
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
                3F, 1F, new ModelRuntimeData(), new AnimationEffects(), () -> {
                });
        controller.tickAnimation(data, Map.of("slot.main", child.name()), new MolangScope(),
                4F, 1F, new ModelRuntimeData(), new AnimationEffects(), () -> {
                });

        assertEquals(3F, data.getStartTick(), 0.0001F);
        assertNotNull(data.getCurrState());
        assertEquals(child.name(), data.owner().currentAnimations().get("slot.main"));
        assertEquals(1, createdStates.get());
    }

    private record TestAnimation(String name, AtomicInteger createdStates) implements Animation<Object> {
        @Override
        public void onFinish(Object data) {
        }

        @Override
        public boolean anyAnimationFinished(Object data) {
            return false;
        }

        @Override
        public boolean allAnimationFinished(Object data) {
            return false;
        }

        @Override
        public Object createData() {
            createdStates.incrementAndGet();
            return new Object();
        }

        @Override
        public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                                  ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback) {
        }
    }
}
