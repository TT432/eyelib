package io.github.tt432.eyelib.util.data_attach;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public interface IDataAttachmentContainer extends INBTSerializable<CompoundTag> {
    <T> boolean has(DataAttachment<T> attachment);

    <T> T get(DataAttachment<T> attachment);

    <T> void set(DataAttachment<T> attachment, T value);

    <T> void remove(DataAttachment<T> attachment);
}
