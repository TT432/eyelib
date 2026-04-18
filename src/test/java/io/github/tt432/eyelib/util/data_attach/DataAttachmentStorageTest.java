package io.github.tt432.eyelib.util.data_attach;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataAttachmentStorageTest {
    @Test
    void mapStorageSupportsSetGetHasAndRemove() {
        DataAttachmentMapStorage storage = new DataAttachmentMapStorage();
        DataAttachmentType<Integer> attachment = new DataAttachmentType<>(
                "eyelib:test_int",
                () -> 0,
                null,
                null
        );

        storage.set(attachment, 7);

        assertTrue(storage.has(attachment));
        assertEquals(7, storage.get(attachment));

        storage.remove(attachment);

        assertFalse(storage.has(attachment));
        assertNull(storage.get(attachment));
    }

    @Test
    void getOrCreateCreatesOnceAndReusesStoredValue() {
        AtomicInteger createCount = new AtomicInteger();
        DataAttachmentType<StringBuilder> attachment = new DataAttachmentType<>(
                "eyelib:test_builder",
                () -> {
                    createCount.incrementAndGet();
                    return new StringBuilder("created");
                },
                null,
                null
        );
        DataAttachmentMapStorage storage = new DataAttachmentMapStorage();

        StringBuilder first = storage.getOrCreate(attachment);
        StringBuilder second = storage.getOrCreate(attachment);

        assertSame(first, second);
        assertEquals(1, createCount.get());
    }

    @Test
    void getOrCreateReplacesNullReadWhenHasIsTrue() {
        DataAttachmentType<Integer> attachment = new DataAttachmentType<>(
                "eyelib:test_default_method",
                () -> 42,
                null,
                null
        );
        NullWhenReadStorage storage = new NullWhenReadStorage();

        Integer result = storage.getOrCreate(attachment);

        assertEquals(42, result);
        assertEquals(42, storage.stored);
    }

    private static final class NullWhenReadStorage implements DataAttachmentStorage {
        private Integer stored;

        @Override
        public <T> boolean has(DataAttachmentType<T> attachment) {
            return true;
        }

        @Override
        public <T> @Nullable T get(DataAttachmentType<T> attachment) {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> void set(DataAttachmentType<T> attachment, T value) {
            stored = (Integer) value;
        }

        @Override
        public <T> void remove(DataAttachmentType<T> attachment) {
            stored = null;
        }
    }
}

