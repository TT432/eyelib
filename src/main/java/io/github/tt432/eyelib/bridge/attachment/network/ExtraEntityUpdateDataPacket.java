package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.util.entitydata.ExtraEntityUpdateData;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record ExtraEntityUpdateDataPacket(
        int entityId,
        ExtraEntityUpdateData data
) /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
    public static final StreamCodec<ExtraEntityUpdateDataPacket> STREAM_CODEC = new StreamCodec<ExtraEntityUpdateDataPacket>() {
        @Override
        public void encode(ExtraEntityUpdateDataPacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            ExtraEntityUpdateData.STREAM_CODEC.encode(obj.data, buf);
        }

        @Override
        public ExtraEntityUpdateDataPacket decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var data = ExtraEntityUpdateData.STREAM_CODEC.decode(buf);
            return new ExtraEntityUpdateDataPacket(entityId, data);
        }
    };

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ExtraEntityUpdateDataPacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    //? if <26.1 {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eyelib", "extra_entity_update_data"));
                    //?} else {
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "extra_entity_update_data"));
                    //?}

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
