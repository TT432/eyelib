package io.github.tt432.eyelib.bridge.attachment.dataattach.mc.adapter;

import io.github.tt432.eyelib.util.dataattach.DataAttachment;
import io.github.tt432.eyelib.util.dataattach.DataAttachmentContainer;
import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;
//? if >=1.20.6
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
//? if <1.20.6 {
import net.minecraftforge.common.util.INBTSerializable;
//?} elif <26.1 {
import net.neoforged.neoforge.common.util.INBTSerializable;
//?}
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft NBT 序列化支持的数据附属容器。
 *
 * @author TT432
 */
public class McDataAttachmentContainer extends DataAttachmentContainer
        //? if <26.1
        implements INBTSerializable<CompoundTag>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(McDataAttachmentContainer.class);

    //? if <1.20.6 {
    @Override
    public CompoundTag serializeNBT() {
    //?} elif <26.1 {
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
    //?} else {
    public CompoundTag serializeNBT(HolderLookup.@Nullable Provider provider) {
    //?}
        var tag = new CompoundTag();
        for (var entry : attachments.entrySet()) {
            tag.put(entry.getKey(), serializeAttachment(entry.getValue()));
        }
        return tag;
    }

    //? if <1.20.6 {
    @Override
    public void deserializeNBT(CompoundTag nbt) {
    //?} elif <26.1 {
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
    //?} else {
    public void deserializeNBT(HolderLookup.@Nullable Provider provider, CompoundTag nbt) {
    //?}
        //? if <26.1 {
        for (var key : nbt.getAllKeys()) {
        //?} else {
        for (var key : nbt.keySet()) {
        //?}
            var value = nbt.get(key);
            if (value == null) {
                continue;
            }
            DataAttachmentType<?> type = DataAttachmentTypeRegistry.getById(key);
            if (type == null) {
                LOGGER.warn("Skip unknown data attachment id while reading NBT: {}", key);
                continue;
            }
            DataAttachment<?> attachment = attachments.computeIfAbsent(type.id(), k -> new DataAttachment<>(type));
            deserializeAttachmentUnchecked(attachment, value);
        }
    }

    private static <T> Tag serializeAttachment(DataAttachment<T> attachment) {
        var codec = attachment.getType().codec();
        if (codec == null) {
            return new CompoundTag();
        }
        var result = codec.encodeStart(NbtOps.INSTANCE, attachment.getData());
        //? if <1.20.6 {
        return result.getOrThrow(false, LOGGER::warn);
        //?} else {
        return result.getOrThrow(message -> {
            LOGGER.warn(message);
            return new RuntimeException(message);
        });
        //?}
    }

    private static <T> void deserializeAttachment(DataAttachment<T> attachment, Tag nbt) {
        DataAttachmentType<T> type = attachment.getType();
        var codec = type.codec();
        if (codec == null) {
            return;
        }
        //? if <1.20.6 {
        var decoded = codec.decode(NbtOps.INSTANCE, nbt).getOrThrow(false, LOGGER::warn);
        //?} else {
        var decoded = codec.decode(NbtOps.INSTANCE, nbt).getOrThrow(message -> {
            LOGGER.warn(message);
            return new RuntimeException(message);
        });
        //?}
        attachment.setData(decoded.getFirst());
    }

    @SuppressWarnings("unchecked")
    private static void deserializeAttachmentUnchecked(DataAttachment<?> attachment, Tag nbt) {
        deserializeAttachment((DataAttachment<Object>) attachment, nbt);
    }
}

