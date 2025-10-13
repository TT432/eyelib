package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.registries.RegistryObject;

/**
 * @author TT432
 */
public record UniDataUpdatePacket<T>(int entityId, DataAttachmentType<T> type, T data) {

    public static <T> UniDataUpdatePacket<T> crate(Entity entity, RegistryObject<DataAttachmentType<T>> holder) {
        var type = holder.get();
        var data = DataAttachmentHelper.getOrCreate(type, entity);
        return crate(entity.getId(), type, data);
    }

    public static <T> UniDataUpdatePacket<T> crate(int entityId, DataAttachmentType<T> type, T data) {
        return new UniDataUpdatePacket<>(entityId, type, data);
    }

    private void encode(FriendlyByteBuf buf) {
        type.getStreamCodec().encode(data, buf);
    }

    public static final StreamCodec<UniDataUpdatePacket<?>> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(UniDataUpdatePacket<?> obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            EyelibStreamCodecs.RESOURCE_LOCATION.encode(obj.type.id(), buf);
            obj.encode(buf);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        @Override
        public UniDataUpdatePacket<?> decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var id = EyelibStreamCodecs.RESOURCE_LOCATION.decode(buf);
            var type = EyelibAttachableData.getById(id);
            var data = type.getStreamCodec().decode(buf);
            return new UniDataUpdatePacket(entityId, type, data);
        }
    };
}
