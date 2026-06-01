package io.github.tt432.eyelibattachment.dataattach;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.streamcodec.StreamCodec;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 数据附属的类型定义。
 *
 * @param id          附属 ID
 * @param factory     工厂方法
 * @param codec       （反）序列化器（null 表示不持久化）
 * @param streamCodec 用于网络同步的编解码器（null 表示不同步）
 * @param <C>         附属类型
 * @author TT432
 */
public record DataAttachmentType<C>(String id,
                                     Supplier<C> factory,
                                     @Nullable Codec<C> codec,
                                     @Nullable StreamCodec<C> streamCodec) {

    public StreamCodec<C> getStreamCodec() {
        if (streamCodec == null) {
            throw new IllegalStateException(id + " has no StreamCodec");
        }
        return streamCodec;
    }
}