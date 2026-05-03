package io.github.tt432.eyelibattachment.codec.stream;

import net.minecraft.network.FriendlyByteBuf;

@FunctionalInterface
public interface StreamEncoder<T> {
    void encode(T obj, FriendlyByteBuf buf);
}
