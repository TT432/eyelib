package io.github.tt432.eyelib.util.data_attach;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class DataAttachmentContainer implements IDataAttachmentContainer {

    private final Map<ResourceLocation, Tag> attachments = new HashMap<>();

    @Override
    public <T> boolean has(DataAttachment<T> attachment) {
        return false;
    }

    @Override
    public <T> T get(DataAttachment<T> attachment) {
        return null;
    }

    @Override
    public <T> void set(DataAttachment<T> attachment, T value) {

    }

    @Override
    public <T> void remove(DataAttachment<T> attachment) {

    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        for (var entry : attachments.entrySet()) {
            tag.put(entry.getKey().toString(), entry.getValue());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (var key : nbt.getAllKeys()) {
            var id = ResourceLocation.parse(key);
            var value = nbt.get(key);
            attachments.put(id, value);
        }
    }
}
