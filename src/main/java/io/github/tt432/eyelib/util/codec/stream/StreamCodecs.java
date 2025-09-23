package io.github.tt432.eyelib.util.codec.stream;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class StreamCodecs {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final StreamCodec<String> STRING = StreamCodec.create((obj, buf) -> buf.writeUtf(obj), FriendlyByteBuf::readUtf);

    public static <K, V> StreamCodec<Map<K, V>> createForMap(StreamCodec<K> keyCodec, StreamCodec<V> valueCodec) {
        return new StreamCodec<>() {
            @Override
            public void encode(Map<K, V> obj, FriendlyByteBuf buf) {
                buf.writeVarInt(obj.size());
                for (var entry : obj.entrySet()) {
                    keyCodec.encode(entry.getKey(), buf);
                    valueCodec.encode(entry.getValue(), buf);
                }
            }

            @Override
            public Map<K, V> decode(FriendlyByteBuf buf) {
                var map = new HashMap<K, V>();
                for (var count = 0; count < buf.readVarInt(); count++) {
                    map.put(keyCodec.decode(buf), valueCodec.decode(buf));
                }
                return map;
            }
        };
    }

    public static <T> StreamCodec<T> fromCodec(Codec<T> codec) {
        return fromCodec(codec, () -> new NbtAccounter(2097152L));
    }

    static <T> StreamCodec<T> fromCodec(Codec<T> codec, Supplier<NbtAccounter> supplier) {
        return tagCodec(supplier).map(o -> codec.encodeStart(NbtOps.INSTANCE, o).getOrThrow(false, s -> LOGGER.error("Failed to encode: {} {}", s, o)),
                tag -> codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, s -> LOGGER.error("Failed to decode: {} {}", s, tag)));
    }

    static StreamCodec<Tag> tagCodec(final Supplier<NbtAccounter> supplier) {
        return new StreamCodec<>() {
            @Override
            public Tag decode(FriendlyByteBuf buf) {
                Tag tag = buf.readNbt(supplier.get());
                if (tag == null) {
                    throw new DecoderException("Expected non-null compound tag");
                } else {
                    return tag;
                }
            }

            @Override
            public void encode(Tag tag, FriendlyByteBuf buf) {
                if (tag == EndTag.INSTANCE) {
                    throw new EncoderException("Expected non-null compound tag");
                } else {
                    buf.writeNbt((CompoundTag) tag);
                }
            }
        };
    }
}
