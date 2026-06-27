package io.github.tt432.eyelib.bridge.attachment.network;

import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record UpdateDestroyInfoPacket(
        boolean dig
) /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
    public static final StreamCodec<UpdateDestroyInfoPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(UpdateDestroyInfoPacket obj, FriendlyByteBuf buf) {
            buf.writeBoolean(obj.dig);
        }

        @Override
        public UpdateDestroyInfoPacket decode(FriendlyByteBuf buf) {
            var dig = buf.readBoolean();
            return new UpdateDestroyInfoPacket(dig);
        }
    };

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<UpdateDestroyInfoPacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "update_destroy_info"));

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
