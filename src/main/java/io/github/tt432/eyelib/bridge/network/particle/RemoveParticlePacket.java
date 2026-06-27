package io.github.tt432.eyelib.bridge.network.particle;

import net.minecraft.network.FriendlyByteBuf;

/** @author TT432 */
public record RemoveParticlePacket(
        String removeId
) /*? if >=1.20.6 {*/ implements net.minecraft.network.protocol.common.custom.CustomPacketPayload /*?}*/ {
    public static final ParticleStreamCodec<RemoveParticlePacket> STREAM_CODEC = new ParticleStreamCodec<>() {
        @Override
        public void encode(RemoveParticlePacket packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.removeId);
        }

        @Override
        public RemoveParticlePacket decode(FriendlyByteBuf buf) {
            String removeId = buf.readUtf();
            return new RemoveParticlePacket(removeId);
        }
    };

    //? if >=1.20.6 {
    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<RemoveParticlePacket> TYPE =
            new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("eyelib", "remove_particle"));

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    //?}
}
