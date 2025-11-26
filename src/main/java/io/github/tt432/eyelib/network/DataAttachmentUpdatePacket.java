package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import net.minecraft.network.FriendlyByteBuf;

public record DataAttachmentUpdatePacket<C>(int entityId, DataAttachmentType<C> attachment, C value) {
    public static final StreamCodec<DataAttachmentUpdatePacket<Object>> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(DataAttachmentUpdatePacket<Object> obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId(), buf);
            EyelibStreamCodecs.RESOURCE_LOCATION.encode(obj.attachment().id(), buf);
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
            var id = EyelibStreamCodecs.RESOURCE_LOCATION.decode(buf);
            var attachment = (DataAttachmentType<Object>) EyelibAttachableData.REGISTRY.get().getValue(id);
            if (attachment == null) {
                throw new IllegalStateException("Unknown DataAttachmentType id: " + id);
            }
            var codec = attachment.streamCodec();
            if (codec == null) {
                throw new IllegalStateException("DataAttachmentType " + id + " has no StreamCodec");
            }
            var value = codec.decode(buf);
            return new DataAttachmentUpdatePacket<>(entityId, attachment, value);
        }
    };


}
