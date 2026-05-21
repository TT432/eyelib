package io.github.tt432.eyelibattachment.network;

import io.github.tt432.eyelibattachment.capability.ExtraEntityUpdateData;
import io.github.tt432.eyelibutil.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelibutil.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
/** @author TT432 */
public record ExtraEntityUpdateDataPacket(
        int entityId,
        ExtraEntityUpdateData data
) {
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
}