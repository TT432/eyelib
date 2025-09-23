package io.github.tt432.eyelib.util.codec.stream;

import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Function;

public abstract class StreamCodec<T> {
    public abstract void encode(T obj, FriendlyByteBuf buf);

    public abstract T decode(FriendlyByteBuf buf);

    public static <T> StreamCodec<T> create(StreamEncoder<T> encoder, StreamDecoder<T> decoder) {
        return new StreamCodec<>() {
            @Override
            public void encode(T obj, FriendlyByteBuf buf) {
                encoder.encode(obj, buf);
            }

            @Override
            public T decode(FriendlyByteBuf buf) {
                return decoder.decode(buf);
            }
        };
    }

    public <O> StreamCodec<O> map(final Function<? super O, ? extends T> mapEncoder, final Function<? super T, ? extends O> mapDecoder) {
        return new StreamCodec<>() {
            @Override
            public void encode(O obj, FriendlyByteBuf buf) {
                StreamCodec.this.encode(mapEncoder.apply(obj), buf);
            }

            @Override
            public O decode(FriendlyByteBuf buf) {
                return mapDecoder.apply(StreamCodec.this.decode(buf));
            }
        };
    }
}
