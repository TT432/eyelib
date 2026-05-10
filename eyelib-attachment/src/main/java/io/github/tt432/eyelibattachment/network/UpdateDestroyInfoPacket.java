package io.github.tt432.eyelibattachment.network;

import io.github.tt432.eyelibutil.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record UpdateDestroyInfoPacket(
        boolean dig
) {
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
}
