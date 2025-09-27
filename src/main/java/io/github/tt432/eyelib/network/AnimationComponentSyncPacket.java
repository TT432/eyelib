package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.capability.component.AnimationComponent;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record AnimationComponentSyncPacket(
        int entityId,
        AnimationComponent.SerializableInfo animationInfo
) {
    public static final StreamCodec<AnimationComponentSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(AnimationComponentSyncPacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            AnimationComponent.SerializableInfo.STREAM_CODEC.encode(obj.animationInfo, buf);
        }

        @Override
        public AnimationComponentSyncPacket decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var animationInfo = AnimationComponent.SerializableInfo.STREAM_CODEC.decode(buf);
            return new AnimationComponentSyncPacket(entityId, animationInfo);
        }
    };
}
