package io.github.tt432.eyelib.util.event.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author TT432
 */
class RenderStageSubscriberRegistryTest {

    private static final AtomicInteger CALL_COUNT = new AtomicInteger();
    private static final AtomicInteger SECOND_CALL_COUNT = new AtomicInteger();

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
