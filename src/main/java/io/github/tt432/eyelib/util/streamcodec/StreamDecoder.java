package io.github.tt432.eyelib.util.streamcodec;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 流解码函数式接口，从 FriendlyByteBuf 解码出指定类型。
 *
 * @author TT432
 */
@FunctionalInterface
public interface StreamDecoder<T> {
    T decode(FriendlyByteBuf buf);
}