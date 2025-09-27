package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
public record ModelComponentSyncPacket(
        int entityId,
        List<ModelComponent.SerializableInfo> modelInfo
) {
    public static final StreamCodec<ModelComponentSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        private static final StreamCodec<List<ModelComponent.SerializableInfo>> LIST_CODEC = EyelibStreamCodecs.list(ArrayList::new, ModelComponent.SerializableInfo.STREAM_CODEC);

        @Override
        public void encode(ModelComponentSyncPacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            LIST_CODEC.encode(obj.modelInfo, buf);
        }

        @Override
        public ModelComponentSyncPacket decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var modelInfo = LIST_CODEC.decode(buf);
            return new ModelComponentSyncPacket(entityId, modelInfo);
        }
    };
}
