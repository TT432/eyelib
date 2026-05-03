package io.github.tt432.eyelib.client.instrument.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EventRingBuffer}.
 * <p>
 * Covers sequential offer/poll, concurrent multi-threaded offer, soft capacity
 * enforcement, and empty-buffer semantics.
 */
class EventRingBufferTest {

    private EventRingBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new EventRingBuffer();
    }

    @AfterEach
    void tearDown() {
        // Drain any remaining events to leave the singleton in a clean state
        // (tests operate on the fresh instance, but poll defensively)
        while (buffer.poll() != null) {
            // drain
        }
    }

    // ---------------------------------------------------------------
    // Test 1: sequential offer & poll in FIFO order
    // ---------------------------------------------------------------

    @Test
    void testOfferAndPoll() {
        for (int i = 0; i < 10; i++) {
            buffer.offer(new InstrumentEvent(
                    "test_type", "test_source", "test_metric",
                    i, "count", null));
        }

        for (int i = 0; i < 10; i++) {
            InstrumentEvent event = buffer.poll();
            assertNotNull(event, "Expected event at position " + i);
            assertEquals(i, event.metricValue(), 0.0,
                    "FIFO order violated at position " + i);
        }

        assertNull(buffer.poll(), "Buffer should be empty after draining all events");
    }

    // ---------------------------------------------------------------
    // Test 2: concurrent multi-threaded offer
    // ---------------------------------------------------------------

    @Test
    void testConcurrentOffer() throws InterruptedException {
        int threadCount = 4;
        int eventsPerThread = 10_000;
        int totalExpected = threadCount * eventsPerThread;

        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Runnable task = () -> {
            try {
                for (int i = 0; i < eventsPerThread; i++) {
                    buffer.offer(new InstrumentEvent(
                            "concurrent", "thread", "count",
                            1.0, "count", null));
                }
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(task, "offer-thread-" + i);
            threads.add(t);
            t.start();
        }

        latch.await();

        assertNull(error.get(), "No exception should have occurred: " + error.get());

        int polled = 0;
        while (buffer.poll() != null) {
            polled++;
        }

        assertEquals(totalExpected, polled,
                "All " + totalExpected + " events should be pollable");
    }

    // ---------------------------------------------------------------
    // Test 3: soft capacity limit
    // ---------------------------------------------------------------

    @Test
    void testSoftCapacityLimit() {
        int overCapacity = EventRingBuffer.SOFT_CAPACITY + 50_000;
        for (int i = 0; i < overCapacity; i++) {
            buffer.offer(new InstrumentEvent(
                    "capacity", "test", "overflow",
                    i, "count", null));
        }

        int size = buffer.size();
        assertTrue(size <= EventRingBuffer.SOFT_CAPACITY,
                "Buffer size (" + size + ") must not exceed soft capacity (" +
                        EventRingBuffer.SOFT_CAPACITY + ")");

        // The buffer should contain at most SOFT_CAPACITY events, and they
        // should be the most recent ones (oldest were dropped).
        int actualCount = 0;
        InstrumentEvent event;
        while ((event = buffer.poll()) != null) {
            actualCount++;
            // The first retained event should have a metricValue consistent
            // with the drop threshold
            if (actualCount == 1) {
                assertTrue(event.metricValue() >= overCapacity - EventRingBuffer.SOFT_CAPACITY,
                        "First retained event should be among the most recent");
            }
        }

        assertTrue(actualCount <= EventRingBuffer.SOFT_CAPACITY,
                "Actual polled count (" + actualCount + ") must not exceed soft capacity (" +
                        EventRingBuffer.SOFT_CAPACITY + ")");
    }

    // ---------------------------------------------------------------
    // Test 4: isEmpty semantics
    // ---------------------------------------------------------------

    @Test
    void testIsEmpty() {
        assertTrue(buffer.isEmpty(), "Fresh buffer should be empty");

        buffer.offer(new InstrumentEvent(
                "empty_test", "test", "flag",
                1.0, "count", null));
        assertFalse(buffer.isEmpty(), "Buffer should not be empty after offer");

        buffer.poll();
        assertTrue(buffer.isEmpty(), "Buffer should be empty after polling the only event");
    }
}
