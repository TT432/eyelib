package io.github.tt432.eyelib.util.data_attach;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

class DataAttachmentMapStorage implements DataAttachmentStorage {
    protected final Map<String, DataAttachment<?>> attachments = new HashMap<>();

    @Override
    public <T> boolean has(DataAttachmentType<T> attachment) {
        return attachments.containsKey(attachment.id());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @Nullable T get(DataAttachmentType<T> attachment) {
        DataAttachment<T> data = (DataAttachment<T>) attachments.get(attachment.id());
        return data != null ? data.getData() : null;
    }

    @Override
    public <T> void set(DataAttachmentType<T> attachment, @NotNull T value) {
        attachments.put(attachment.id(), new DataAttachment<>(attachment, value));
    }

    @Override
    public <T> void remove(DataAttachmentType<T> attachment) {
        attachments.remove(attachment.id());
    }
}
