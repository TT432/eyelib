package io.github.tt432.eyelib.util.codec.stream;

import net.minecraft.network.FriendlyByteBuf;

@FunctionalInterface
public interface StreamDecoder<T> {
    T decode(FriendlyByteBuf buf);
}
