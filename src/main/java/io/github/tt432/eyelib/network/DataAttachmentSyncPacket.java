package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record DataAttachmentSyncPacket(int entityId, CompoundTag data) {
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
}
