package io.github.tt432.eyelib.util.data_attach;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DataAttachmentContainer implements IDataAttachmentContainer {

    private final Map<ResourceLocation, DataAttachment<?>> attachments = new HashMap<>();

    @Override
    public <T> boolean has(DataAttachmentType<T> attachment) {
        return attachments.containsKey(attachment.id());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @Nullable T get(DataAttachmentType<T> attachment) {
        return ((DataAttachment<T>) attachments.get(attachment.id())).getData();
    }

    @Override
    public <T> void set(DataAttachmentType<T> attachment, @NotNull T value) {
        attachments.put(attachment.id(), new DataAttachment<>(attachment, value));
    }

    @Override
    public <T> void remove(DataAttachmentType<T> attachment) {
        attachments.remove(attachment.id());
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        for (var entry : attachments.entrySet()) {
            tag.put(entry.getKey().toString(), entry.getValue().serializeNBT());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (var key : nbt.getAllKeys()) {
            var id = ResourceLocation.parse(key);
            var value = nbt.get(key);
            var type = EyelibAttachableData.getById(id);
            var attachment = new DataAttachment<>(type);
            attachment.deserializeNBT(value);
            attachments.put(id, attachment);
        }
    }
}
