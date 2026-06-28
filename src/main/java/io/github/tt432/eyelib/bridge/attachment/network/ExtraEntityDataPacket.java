package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.util.entitydata.ExtraEntityData;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record ExtraEntityDataPacket(
        int entityId,
        ExtraEntityData data
) /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
    public static final StreamCodec<ExtraEntityDataPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ExtraEntityDataPacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            ExtraEntityData.STREAM_CODEC.encode(obj.data, buf);
        }

        @Override
        public ExtraEntityDataPacket decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var data = ExtraEntityData.STREAM_CODEC.decode(buf);
            return new ExtraEntityDataPacket(entityId, data);
        }
    };

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<ExtraEntityDataPacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    //? if <26.1 {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eyelib", "extra_entity_data"));
                    //?} else {
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "extra_entity_data"));
                    //?}

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
