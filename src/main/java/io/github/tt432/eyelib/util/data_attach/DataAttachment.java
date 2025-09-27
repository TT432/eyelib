package io.github.tt432.eyelib.util.data_attach;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataAttachment<C> implements INBTSerializable<Tag> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataAttachment.class);

    @Getter
    private final DataAttachmentType<C> type;

    @Getter
    private C data;

    public DataAttachment(DataAttachmentType<C> type) {
        this(type, type.factory().get());
    }

    public DataAttachment(DataAttachmentType<C> type, C data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public Tag serializeNBT() {
        if (type.codec() != null) {
            var result = type.codec().encodeStart(NbtOps.INSTANCE, data);
            // XXX: Recoverable fail.
            return result.getOrThrow(false, LOGGER::warn);
        }

        // XXX: could we return a null?
        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(Tag nbt) {
        if (type.codec() != null) {
            var result = type.codec().decode(NbtOps.INSTANCE, nbt);
            var cTagPair = result.getOrThrow(false, LOGGER::warn);
            // XXX: Recoverable fail.
            data = cTagPair.getFirst();
        }
    }
}
