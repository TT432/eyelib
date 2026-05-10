package io.github.tt432.eyelibutil.streamcodec;

import net.minecraft.network.FriendlyByteBuf;

@FunctionalInterface
public interface StreamEncoder<T> {
    void encode(T obj, FriendlyByteBuf buf);
}
