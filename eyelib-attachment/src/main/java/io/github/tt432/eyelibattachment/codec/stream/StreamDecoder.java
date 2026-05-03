package io.github.tt432.eyelibattachment.codec.stream;

import net.minecraft.network.FriendlyByteBuf;

@FunctionalInterface
public interface StreamDecoder<T> {
    T decode(FriendlyByteBuf buf);
}
