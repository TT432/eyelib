package io.github.tt432.eyelib.network.packet;

import io.github.tt432.eyelibattachment.capability.AnimationComponentInfo;
import io.github.tt432.eyelibutil.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelibutil.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
public record AnimationComponentSyncPacket(
        int entityId,
        AnimationComponentInfo animationInfo
) {
    public static final StreamCodec<AnimationComponentSyncPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(AnimationComponentSyncPacket obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.entityId, buf);
            AnimationComponentInfo.STREAM_CODEC.encode(obj.animationInfo, buf);
        }

        @Override
        public AnimationComponentSyncPacket decode(FriendlyByteBuf buf) {
            var entityId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var animationInfo = AnimationComponentInfo.STREAM_CODEC.decode(buf);
            return new AnimationComponentSyncPacket(entityId, animationInfo);
        }
    };
}
