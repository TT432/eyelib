package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record UniDataUpdatePacket<T>(int entityId, DataAttachmentType<T> attachmentType, T data)
        /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {

    public static <T> UniDataUpdatePacket<T> crate(int entityId, DataAttachmentType<T> type, T data) {
        return new UniDataUpdatePacket<>(entityId, type, data);
    }

    private void encode(FriendlyByteBuf buf) {
        attachmentType.getStreamCodec().encode(data, buf);
    }

    public static final StreamCodec<UniDataUpdatePacket<?>> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(UniDataUpdatePacket<?> obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            EyelibStreamCodecs.STRING.encode(obj.attachmentType.id(), buf);
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

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<UniDataUpdatePacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    //? if <26.1 {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eyelib", "uni_data_update"));
                    //?} else {
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "uni_data_update"));
                    //?}

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
