package io.github.tt432.eyelib.util.data_attach;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IDataAttachmentContainer extends INBTSerializable<CompoundTag> {
    <T> boolean has(DataAttachmentType<T> attachment);

    <T> @Nullable T get(DataAttachmentType<T> attachment);

    default <T> T getOrCreate(DataAttachmentType<T> attachment) {
        if (has(attachment)) {
            return get(attachment);
        } else {
            T result = attachment.factory().get();
            set(attachment, result);
            return result;
        }
    }

    <T> void set(DataAttachmentType<T> attachment, @NotNull T value);

    <T> void remove(DataAttachmentType<T> attachment);
}
