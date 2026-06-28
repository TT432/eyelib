package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record DataAttachmentUpdatePacket<C>(int entityId, DataAttachmentType<C> attachment, C value)
        /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
    public static final StreamCodec<DataAttachmentUpdatePacket<Object>> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(DataAttachmentUpdatePacket<Object> obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId(), buf);
            EyelibStreamCodecs.STRING.encode(obj.attachment().id(), buf);
            var codec = obj.attachment().streamCodec();
            if (codec == null) {
                throw new IllegalStateException("DataAttachmentType " + obj.attachment().id() + " has no StreamCodec");
            }
            codec.encode(obj.value(), buf);
        }

        @SuppressWarnings("unchecked")
        @Override
        public DataAttachmentUpdatePacket<Object> decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var id = EyelibStreamCodecs.STRING.decode(buf);
            var attachment = (DataAttachmentType<Object>) DataAttachmentTypeRegistry.getById(id);
            var codec = attachment.streamCodec();
            if (codec == null) {
                throw new IllegalStateException("DataAttachmentType " + id + " has no StreamCodec");
            }
            var value = codec.decode(buf);
            return new DataAttachmentUpdatePacket<>(entityId, attachment, value);
        }
    };

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<DataAttachmentUpdatePacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    //? if <26.1 {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eyelib", "data_attachment_update"));
                    //?} else {
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "data_attachment_update"));
                    //?}

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
