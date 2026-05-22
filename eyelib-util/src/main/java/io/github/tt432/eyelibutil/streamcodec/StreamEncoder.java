package io.github.tt432.eyelibutil.streamcodec;

import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
@FunctionalInterface
public interface StreamEncoder<T> {
    void encode(T obj, FriendlyByteBuf buf);
}