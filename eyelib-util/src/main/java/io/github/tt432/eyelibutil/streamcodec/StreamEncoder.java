package io.github.tt432.eyelibutil.streamcodec;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 流编码函数式接口，将指定类型编码到 FriendlyByteBuf。
 *
 * @author TT432
 */
@FunctionalInterface
public interface StreamEncoder<T> {
    void encode(T obj, FriendlyByteBuf buf);
}