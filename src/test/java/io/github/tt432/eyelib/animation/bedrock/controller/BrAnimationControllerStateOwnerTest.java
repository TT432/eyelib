package io.github.tt432.eyelib.animation.bedrock.controller;

import io.github.tt432.eyelib.animation.Animation;
import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationManager;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** @author TT432 */
class BrAnimationControllerStateOwnerTest {
    @AfterEach
    void tearDown() {
        AnimationManager.INSTANCE.clear();
    }

    @Test
    void tickAnimationInitializesControllerState() {
        AtomicInteger createdStates = new AtomicInteger();
        TestAnimation child = new TestAnimation("animation.test.child", createdStates);
        AnimationManager.INSTANCE.put(child.name(), child);

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

        assertEquals(3F, data.getStartTick(), 0.0001F);
        assertNotNull(data.getCurrState());
        assertEquals(child.name(), data.owner().currentAnimations().get("slot.main"));
    }

    @Test
    void tickAnimationCachesChildAnimationState() {
        AtomicInteger createdStates = new AtomicInteger();
        TestAnimation child = new TestAnimation("animation.test.child", createdStates);
        AnimationManager.INSTANCE.put(child.name(), child);

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

        assertEquals(1, createdStates.get());
    }

    @Test
    void getDataDelegatesToAnimationCreateData() {
        AtomicInteger createDataCalls = new AtomicInteger();
        Object stateData = new Object();

        Animation animation = new TestGetDataAnimation(stateData, createDataCalls, new AtomicInteger());

        BrControllerStateOwner owner = new BrControllerStateOwner();

        Object result1 = owner.getData(animation);
        assertNotNull(result1, "getData should return non-null data for a valid animation");
        assertSame(stateData, result1, "getData should return the object created by createData()");
        assertEquals(1, createDataCalls.get(), "createData should be called exactly once after first getData");
    }

    @Test
    void getDataCachesResultByAnimationName() {
        AtomicInteger createDataCalls = new AtomicInteger();
        AtomicInteger nameCalls = new AtomicInteger();
        Object stateData = new Object();

        Animation animation = new TestGetDataAnimation(stateData, createDataCalls, nameCalls);

        BrControllerStateOwner owner = new BrControllerStateOwner();

        Object result1 = owner.getData(animation);
        int nameCallsAfterFirst = nameCalls.get();

        Object result2 = owner.getData(animation);

        assertSame(result1, result2, "Repeated getData must return the same cached object");
        assertEquals(1, createDataCalls.get(), "createData must NOT be called again on second getData");
        assertEquals(nameCallsAfterFirst + 1, nameCalls.get(),
                "name() should be called on each getData invocation as the map key");
    }

    private record TestAnimation(String name, AtomicInteger createdStates) implements Animation {
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

    private record TestGetDataAnimation(Object stateData, AtomicInteger createDataCalls, AtomicInteger nameCalls) implements Animation {
        @Override
        public String name() {
            nameCalls.incrementAndGet();
            return "animation.test.getdata";
        }

        @Override
        public Object createData() {
            createDataCalls.incrementAndGet();
            return stateData;
        }

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
        public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope,
                                  float ticks, float multiplier, ModelRuntimeData renderInfos,
                                  AnimationEffects effects, Runnable animationStartFeedback) {
        }
    }
}
