package io.github.tt432.eyelibattachment.dataattach.mc;

import io.github.tt432.eyelibattachment.dataattach.DataAttachment;
import io.github.tt432.eyelibattachment.dataattach.DataAttachmentContainer;
import io.github.tt432.eyelibattachment.dataattach.DataAttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minecraft NBT 序列化支持的数据附属容器。
 *
 * @author TT432
 */
public class McDataAttachmentContainer extends DataAttachmentContainer implements INBTSerializable<CompoundTag> {
    private static final Logger LOGGER = LoggerFactory.getLogger(McDataAttachmentContainer.class);

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        for (var entry : attachments.entrySet()) {
            tag.put(entry.getKey(), serializeAttachment(entry.getValue()));
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        for (var key : nbt.getAllKeys()) {
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
        return result.getOrThrow(false, LOGGER::warn);
    }

    private static <T> void deserializeAttachment(DataAttachment<T> attachment, Tag nbt) {
        DataAttachmentType<T> type = attachment.getType();
        var codec = type.codec();
        if (codec == null) {
            return;
        }
        var decoded = codec.decode(NbtOps.INSTANCE, nbt).getOrThrow(false, LOGGER::warn);
        attachment.setData(decoded.getFirst());
    }

    @SuppressWarnings("unchecked")
    private static void deserializeAttachmentUnchecked(DataAttachment<?> attachment, Tag nbt) {
        deserializeAttachment((DataAttachment<Object>) attachment, nbt);
    }
}
