package io.github.tt432.eyelibutil.streamcodec;

import net.minecraft.network.FriendlyByteBuf;

/**
 * @author TT432
 */
@FunctionalInterface
public interface StreamDecoder<T> {
    T decode(FriendlyByteBuf buf);
}