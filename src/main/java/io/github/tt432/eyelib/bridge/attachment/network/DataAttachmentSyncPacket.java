package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record DataAttachmentSyncPacket(int entityId, CompoundTag data)
        /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
    public static final StreamCodec<DataAttachmentSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(DataAttachmentSyncPacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId(), buf);
            EyelibStreamCodecs.COMPOUND_TAG.encode(obj.data, buf);
        }

        @Override
        public DataAttachmentSyncPacket decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var data = EyelibStreamCodecs.COMPOUND_TAG.decode(buf);
            return new DataAttachmentSyncPacket(entityId, data);
        }
    };

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<DataAttachmentSyncPacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    //? if <26.1 {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eyelib", "data_attachment_sync"));
                    //?} else {
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "data_attachment_sync"));
                    //?}

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
