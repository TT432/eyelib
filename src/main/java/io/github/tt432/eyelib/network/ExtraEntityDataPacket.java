package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record ExtraEntityDataPacket(
        int entityId,
        ExtraEntityData data
) {
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
}
