package io.github.tt432.eyelib.util.data_attach;

import org.jspecify.annotations.Nullable;

public interface DataAttachmentStorage {
    <T> boolean has(DataAttachmentType<T> attachment);

    <T> @Nullable T get(DataAttachmentType<T> attachment);

    default <T> T getOrCreate(DataAttachmentType<T> attachment) {
        if (has(attachment)) {
            @Nullable T value = get(attachment);
            if (value != null) {
                return value;
            }
        }
        T result = attachment.factory().get();
        set(attachment, result);
        return result;
    }

    <T> void set(DataAttachmentType<T> attachment, T value);

    <T> void remove(DataAttachmentType<T> attachment);
}

