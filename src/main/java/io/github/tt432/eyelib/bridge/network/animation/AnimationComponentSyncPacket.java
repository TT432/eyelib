package io.github.tt432.eyelib.bridge.network.animation;

import io.github.tt432.eyelib.animation.AnimationComponentInfo;
import io.github.tt432.eyelib.util.streamcodec.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.streamcodec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
/**
 * @author TT432
 */
public record AnimationComponentSyncPacket(
        int entityId,
        AnimationComponentInfo animationInfo
) /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
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

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<AnimationComponentSyncPacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    //? if <26.1 {
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eyelib", "animation_component_sync"));
                    //?} else {
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "animation_component_sync"));
                    //?}

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
