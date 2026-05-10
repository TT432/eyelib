package io.github.tt432.eyelibutil.streamcodec;

import net.minecraft.network.FriendlyByteBuf;

@FunctionalInterface
public interface StreamDecoder<T> {
    T decode(FriendlyByteBuf buf);
}
