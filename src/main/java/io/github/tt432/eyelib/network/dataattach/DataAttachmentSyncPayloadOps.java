package io.github.tt432.eyelib.network.dataattach;

import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.capability.ExtraEntityUpdateData;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.mc.impl.network.packet.DataAttachmentUpdatePacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ExtraEntityDataPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.mc.impl.network.packet.UniDataUpdatePacket;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DataAttachmentSyncPayloadOps {
    public record AttachmentUpdate<T>(int entityId, DataAttachmentType<T> attachment, T value) {
    }

    @SuppressWarnings("unchecked")
    public static AttachmentUpdate<Object> from(DataAttachmentUpdatePacket<?> packet) {
        return new AttachmentUpdate<>(packet.entityId(), (DataAttachmentType<Object>) packet.attachment(), packet.value());
    }

    public static <T> AttachmentUpdate<T> from(UniDataUpdatePacket<T> packet) {
        return new AttachmentUpdate<>(packet.entityId(), packet.type(), packet.data());
    }

    public static AttachmentUpdate<ExtraEntityData> from(ExtraEntityDataPacket packet) {
        return new AttachmentUpdate<>(packet.entityId(), EyelibAttachableData.EXTRA_ENTITY_DATA.get(), packet.data());
    }

    public static AttachmentUpdate<ExtraEntityUpdateData> from(ExtraEntityUpdateDataPacket packet) {
        return new AttachmentUpdate<>(packet.entityId(), EyelibAttachableData.EXTRA_ENTITY_UPDATE.get(), packet.data());
    }

    public static ExtraEntityData withDigState(ExtraEntityData current, boolean dig) {
        if (current.is_dig() == dig) {
            return current;
        }
        return current.with_dig(dig);
    }
}
