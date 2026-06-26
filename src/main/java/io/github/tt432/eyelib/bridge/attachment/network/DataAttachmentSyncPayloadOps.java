package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.util.entitydata.ExtraEntityData;
import io.github.tt432.eyelib.util.entitydata.ExtraEntityUpdateData;
import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 数据附属同步载荷的转换工具。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataAttachmentSyncPayloadOps {
    public record AttachmentUpdate<T>(int entityId, DataAttachmentType<T> attachment, T value) {
    }

    @SuppressWarnings("unchecked")
    public static AttachmentUpdate<Object> from(DataAttachmentUpdatePacket<?> packet) {
        return new AttachmentUpdate<>(packet.entityId(), (DataAttachmentType<Object>) packet.attachment(), packet.value());
    }

    public static <T> AttachmentUpdate<T> from(UniDataUpdatePacket<T> packet) {
        return new AttachmentUpdate<>(packet.entityId(), packet.attachmentType(), packet.data());
    }

    public static AttachmentUpdate<ExtraEntityData> from(ExtraEntityDataPacket packet) {
        return new AttachmentUpdate<>(packet.entityId(), DataAttachmentTypeRegistry.EXTRA_ENTITY_DATA.get(), packet.data());
    }

    public static AttachmentUpdate<ExtraEntityUpdateData> from(ExtraEntityUpdateDataPacket packet) {
        return new AttachmentUpdate<>(packet.entityId(), DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get(), packet.data());
    }

    public static ExtraEntityData withDigState(ExtraEntityData current, boolean dig) {
        if (current.is_dig() == dig) {
            return current;
        }
        return current.with_dig(dig);
    }
}