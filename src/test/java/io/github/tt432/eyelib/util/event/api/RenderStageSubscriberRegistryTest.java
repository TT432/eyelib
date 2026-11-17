package io.github.tt432.eyelib.util.event.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author TT432
 */
class RenderStageSubscriberRegistryTest {

    private static final AtomicInteger CALL_COUNT = new AtomicInteger();
    private static final AtomicInteger SECOND_CALL_COUNT = new AtomicInteger();
    private static final List<String> CALL_SEQUENCE = new CopyOnWriteArrayList<>();

    private static void firstSubscriber(float partialTick, double camX, double camY, double camZ) {
        CALL_COUNT.incrementAndGet();
    }

    private static void secondSubscriber(float partialTick, double camX, double camY, double camZ) {
        SECOND_CALL_COUNT.incrementAndGet();
    }

    private static void failingSubscriber(float partialTick, double camX, double camY, double camZ) {
        throw new RuntimeException("intentional test failure");
    }

    @AfterEach
    void cleanup() {
        RenderStageRegistries.setupRenderStage(discoveryWith());
        CALL_COUNT.set(0);
        SECOND_CALL_COUNT.set(0);
        CALL_SEQUENCE.clear();
    }

    @Test
    void setupRegistersSubscribers() {
        RenderStageRegistries.setupRenderStage(discoveryWith(
                subscriber("firstSubscriber"), subscriber("secondSubscriber")
        ));

        assertEquals(2, RenderStageRegistries.renderStage().subscriberCount());
    }

    @Test
    void dispatchInvokesAllSubscribers() {
        RenderStageRegistries.setupRenderStage(discoveryWith(
                subscriber("firstSubscriber"), subscriber("secondSubscriber")
        ));

        RenderStageRegistries.renderStage().dispatch(1.0f, 2.0, 3.0, 4.0);

        assertEquals(1, CALL_COUNT.get());
        assertEquals(1, SECOND_CALL_COUNT.get());
    }

    @Test
    void failingSubscriberDoesNotBlockOthers() {
        RenderStageRegistries.setupRenderStage(discoveryWith(
                subscriber("failingSubscriber"), subscriber("firstSubscriber")
        ));

        RenderStageRegistries.renderStage().dispatch(1.0f, 2.0, 3.0, 4.0);

        assertEquals(1, CALL_COUNT.get());
    }

    @Test
    void setupClearsPreviousSubscribers() {
        RenderStageRegistries.setupRenderStage(discoveryWith(
                subscriber("firstSubscriber"), subscriber("secondSubscriber")
        ));
        RenderStageRegistries.setupRenderStage(discoveryWith());

        assertEquals(0, RenderStageRegistries.renderStage().subscriberCount());
    }

    @Test
    void emptyDispatchIsSafe() {
        RenderStageRegistries.setupRenderStage(discoveryWith());

        RenderStageRegistries.renderStage().dispatch(0f, 0, 0, 0);
    }

    @Test
    void registerCustomImplementation() {
        RenderStageRegistries.register(new StubRegistry());

        assertEquals(42, RenderStageRegistries.renderStage().subscriberCount());
    }

    @Test
    void priorityOrdersDispatch() {
        // 乱序输入，dispatch 必须按 priority 升序：BEFORE_TICK(-100) → CORE(0) → AFTER_PIPELINE(200)
        RenderStageRegistries.setupRenderStage(discoveryWith(
                subscriber(CoreStageSubscriber.class, "onStage", RenderStagePriority.CORE),
                subscriber(AfterPipelineStageSubscriber.class, "onStage", RenderStagePriority.AFTER_PIPELINE),
                subscriber(BeforeTickStageSubscriber.class, "onStage", RenderStagePriority.BEFORE_TICK)
        ));

        RenderStageRegistries.renderStage().dispatch(1.0f, 2.0, 3.0, 4.0);

        assertEquals(List.of("beforeTick", "core", "afterPipeline"), CALL_SEQUENCE);
    }

    @Test
    void samePriorityIsStableByClassName() {
        // 同优先级的顺序不应取决于发现顺序：两次颠倒输入，输出必须一致（按声明类全名排序）
        RenderStageRegistries.setupRenderStage(discoveryWith(
                subscriber(AlphaStageSubscriber.class, "record"),
                subscriber(BetaStageSubscriber.class, "record")
        ));
        RenderStageRegistries.renderStage().dispatch(0, 0, 0, 0);
        List<String> firstOrder = List.copyOf(CALL_SEQUENCE);

        CALL_SEQUENCE.clear();
        RenderStageRegistries.setupRenderStage(discoveryWith(
                subscriber(BetaStageSubscriber.class, "record"),
                subscriber(AlphaStageSubscriber.class, "record")
        ));
        RenderStageRegistries.renderStage().dispatch(0, 0, 0, 0);

        assertEquals(firstOrder, List.copyOf(CALL_SEQUENCE));
        assertEquals(List.of("alpha", "beta"), CALL_SEQUENCE);
    }

    private static RenderStageSubscriberDiscovery discoveryWith(RenderStageSubscriberDiscovery.RenderStageSubscriber... subscribers) {
        return () -> List.of(subscribers);
    }

    private static RenderStageSubscriberDiscovery.RenderStageSubscriber subscriber(String methodName) {
        try {
            MethodHandle handle = MethodHandles.lookup().findStatic(
                    RenderStageSubscriberRegistryTest.class, methodName,
                    MethodType.methodType(void.class, float.class, double.class, double.class, double.class)
            );
            return new RenderStageSubscriberDiscovery.RenderStageSubscriber(
                    RenderStageSubscriberRegistryTest.class, methodName, handle
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static RenderStageSubscriberDiscovery.RenderStageSubscriber subscriber(
            Class<?> declaringClass, String methodName, int priority) {
        try {
            MethodHandle handle = MethodHandles.lookup().findStatic(
                    declaringClass, methodName,
                    MethodType.methodType(void.class, float.class, double.class, double.class, double.class)
            );
            return new RenderStageSubscriberDiscovery.RenderStageSubscriber(
                    declaringClass, methodName, handle, priority
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static RenderStageSubscriberDiscovery.RenderStageSubscriber subscriber(
            Class<?> declaringClass, String methodName) {
        return subscriber(declaringClass, methodName, RenderStagePriority.CORE);
    }

    /**
     * 同优先级、方法名也相同的两个订阅者，只靠声明类全名区分顺序。
     */
    static final class AlphaStageSubscriber {
        static void record(float partialTick, double camX, double camY, double camZ) {
            CALL_SEQUENCE.add("alpha");
        }
    }

    static final class BetaStageSubscriber {
        static void record(float partialTick, double camX, double camY, double camZ) {
            CALL_SEQUENCE.add("beta");
        }
    }

    static final class CoreStageSubscriber {
        static void onStage(float partialTick, double camX, double camY, double camZ) {
            CALL_SEQUENCE.add("core");
        }
    }

    static final class BeforeTickStageSubscriber {
        static void onStage(float partialTick, double camX, double camY, double camZ) {
            CALL_SEQUENCE.add("beforeTick");
        }
    }

    static final class AfterPipelineStageSubscriber {
        static void onStage(float partialTick, double camX, double camY, double camZ) {
            CALL_SEQUENCE.add("afterPipeline");
        }
    }

    private static final class StubRegistry implements RenderStageSubscriberRegistry {
        @Override
        public void setup(RenderStageSubscriberDiscovery discovery) {
        }

        @Override
        public void dispatch(float partialTick, double camX, double camY, double camZ) {
        }

        @Override
        public int subscriberCount() {
            return 42;
        }
    }
}
