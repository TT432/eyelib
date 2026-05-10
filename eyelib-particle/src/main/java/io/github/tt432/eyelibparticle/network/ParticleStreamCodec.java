package io.github.tt432.eyelibparticle.network;

import net.minecraft.network.FriendlyByteBuf;

public interface ParticleStreamCodec<T> {
    void encode(T packet, FriendlyByteBuf buf);

    T decode(FriendlyByteBuf buf);
}
