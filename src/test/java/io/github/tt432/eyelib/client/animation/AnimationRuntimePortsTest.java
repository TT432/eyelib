package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.client.manager.AnimationManager;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AnimationRuntimePortsTest {
    @AfterEach
    void tearDown() {
        AnimationManager.writePort().clear();
    }

    @Test
    void legacyAnimationPortsDelegateToExistingInterfaceMethods() {
        AtomicInteger finished = new AtomicInteger();
        AtomicInteger anyChecks = new AtomicInteger();
        AtomicInteger allChecks = new AtomicInteger();
        AtomicInteger ticks = new AtomicInteger();
        Object state = new Object();
        TestAnimation animation = new TestAnimation("animation.test.idle", state, finished, anyChecks, allChecks, ticks);

        AnimationRuntimePortSet<Object> ports = animation.ports();

        assertEquals("animation.test.idle", ports.identity().name());
        assertSame(state, ports.state().createData());
        ports.state().onFinish(state);
        ports.state().anyAnimationFinished(state);
        ports.state().allAnimationFinished(state);
        ports.execution().tickAnimation(state, Map.of(), new MolangScope(), 3F, 1F,
                new ModelRuntimeData(), new AnimationEffects(), () -> {
                });

        assertEquals(1, finished.get());
        assertEquals(1, anyChecks.get());
        assertEquals(1, allChecks.get());
        assertEquals(1, ticks.get());
    }

    @Test
    void brAnimatorUsesRuntimePortsWithoutChangingLegacyAnimationExecution() {
        AtomicInteger tickCalls = new AtomicInteger();
        Object[] seenData = new Object[1];
        TestAnimation animation = new TestAnimation("animation.test.walk", new Object(), new AtomicInteger(),
                new AtomicInteger(), new AtomicInteger(), tickCalls) {
            @Override
            public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                                      ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback) {
                seenData[0] = data;
                tickCalls.incrementAndGet();
            }
        };
        AnimationManager.writePort().put(animation.name(), animation);

        AnimationComponent component = new AnimationComponent();
        component.setup(Map.of("controller.main", animation.name()), Map.of("controller.main", MolangValue.ONE));
        Object runtimeData = component.getAnimationData(animation.name());

        BrAnimator.tickAnimation(component, new MolangScope(), new AnimationEffects(), 4F, () -> {
        });

        assertSame(runtimeData, seenData[0]);
        assertEquals(1, tickCalls.get());
    }

    private static class TestAnimation implements Animation<Object> {
        private final String name;
        private final Object state;
        private final AtomicInteger finished;
        private final AtomicInteger anyChecks;
        private final AtomicInteger allChecks;
        private final AtomicInteger ticks;

        private TestAnimation(String name, Object state, AtomicInteger finished, AtomicInteger anyChecks,
                              AtomicInteger allChecks, AtomicInteger ticks) {
            this.name = name;
            this.state = state;
            this.finished = finished;
            this.anyChecks = anyChecks;
            this.allChecks = allChecks;
            this.ticks = ticks;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public void onFinish(Object data) {
            finished.incrementAndGet();
        }

        @Override
        public boolean anyAnimationFinished(Object data) {
            anyChecks.incrementAndGet();
            return false;
        }

        @Override
        public boolean allAnimationFinished(Object data) {
            allChecks.incrementAndGet();
            return false;
        }

        @Override
        public Object createData() {
            return state;
        }

        @Override
        public void tickAnimation(Object data, Map<String, String> animations, MolangScope scope, float ticks, float multiplier,
                                  ModelRuntimeData renderInfos, AnimationEffects effects, Runnable animationStartFeedback) {
            this.ticks.incrementAndGet();
        }
    }
}
