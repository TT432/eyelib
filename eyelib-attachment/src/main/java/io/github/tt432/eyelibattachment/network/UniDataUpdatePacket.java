package io.github.tt432.eyelibattachment.network;

import io.github.tt432.eyelibattachment.dataattach.DataAttachmentType;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelibutil.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelibutil.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
/** @author TT432 */
public record UniDataUpdatePacket<T>(int entityId, DataAttachmentType<T> type, T data) {

    public static <T> UniDataUpdatePacket<T> crate(int entityId, DataAttachmentType<T> type, T data) {
        return new UniDataUpdatePacket<>(entityId, type, data);
    }

    private void encode(FriendlyByteBuf buf) {
        type.getStreamCodec().encode(data, buf);
    }

    public static final StreamCodec<UniDataUpdatePacket<?>> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(UniDataUpdatePacket<?> obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            EyelibStreamCodecs.STRING.encode(obj.type.id(), buf);
            obj.encode(buf);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public UniDataUpdatePacket<?> decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var id = EyelibStreamCodecs.STRING.decode(buf);
            var type = (DataAttachmentType<Object>) DataAttachmentTypeRegistry.getById(id);
            var data = type.getStreamCodec().decode(buf);
            return new UniDataUpdatePacket(entityId, type, data);
        }
    };
}