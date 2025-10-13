package io.github.tt432.eyelib.util.codec.stream;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class EyelibStreamCodecs {
    private static final Logger LOGGER = LogUtils.getLogger();

    // <editor-fold desc="Primitive types">

    public static final StreamCodec<Boolean> BOOL = StreamCodec.create((obj, buf) -> buf.writeBoolean(obj), FriendlyByteBuf::readBoolean);

    public static final StreamCodec<Float> FLOAT = StreamCodec.create((obj, buf) -> buf.writeFloat(obj), FriendlyByteBuf::readFloat);

    public static final StreamCodec<Double> DOUBLE = StreamCodec.create((obj, buf) -> buf.writeDouble(obj), FriendlyByteBuf::readDouble);

    public static final StreamCodec<String> STRING = StreamCodec.create((obj, buf) -> buf.writeUtf(obj), FriendlyByteBuf::readUtf);

    public static final StreamCodec<Integer> VAR_INT = StreamCodec.create((obj, buf) -> buf.writeVarInt(obj), FriendlyByteBuf::readVarInt);

    // </editor-fold>

    // <editor-fold desc="Minecraft types">

    public static final StreamCodec<ResourceLocation> RESOURCE_LOCATION = new StreamCodec<>() {
        @Override
        public void encode(ResourceLocation obj, FriendlyByteBuf buf) {
            STRING.encode(obj.toString(), buf);
        }

        @Override
        public ResourceLocation decode(FriendlyByteBuf buf) {
            var str = STRING.decode(buf);
            return new ResourceLocation(str);
        }
    };

    public static final StreamCodec<Vector3f> VECTOR_3_F = new StreamCodec<>() {
        @Override
        public void encode(Vector3f obj, FriendlyByteBuf buf) {
            FLOAT.encode(obj.x(), buf);
            FLOAT.encode(obj.y(), buf);
            FLOAT.encode(obj.z(), buf);
        }

        @Override
        public Vector3f decode(FriendlyByteBuf buf) {
            var x = FLOAT.decode(buf);
            var y = FLOAT.decode(buf);
            var z = FLOAT.decode(buf);
            return new Vector3f(x, y, z);
        }
    };

    public static StreamCodec<Tag> tag(final Supplier<NbtAccounter> supplier) {
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

    public static <K, V> StreamCodec<Map<K, V>> map(Supplier<Map<K, V>> factory, StreamCodec<K> keyCodec, StreamCodec<V> valueCodec) {
        return new StreamCodec<>() {
            @Override
            public void encode(Map<K, V> obj, FriendlyByteBuf buf) {
                VAR_INT.encode(obj.size(), buf);
                for (var entry : obj.entrySet()) {
                    keyCodec.encode(entry.getKey(), buf);
                    valueCodec.encode(entry.getValue(), buf);
                }
            }

            @Override
            public Map<K, V> decode(FriendlyByteBuf buf) {
                var count = VAR_INT.decode(buf);
                var map = factory.get();
                for (var i = 0; i < count; i++) {
                    map.put(keyCodec.decode(buf), valueCodec.decode(buf));
                }
                return map;
            }
        };
    }

    public static <E> StreamCodec<List<E>> list(Supplier<List<E>> factory, StreamCodec<E> elementCodec) {
        return new StreamCodec<>() {
            @Override
            public void encode(List<E> obj, FriendlyByteBuf buf) {
                VAR_INT.encode(obj.size(), buf);
                for (var e : obj) {
                    elementCodec.encode(e, buf);
                }
            }

            @Override
            public List<E> decode(FriendlyByteBuf buf) {
                var count = VAR_INT.decode(buf);
                var list = factory.get();
                for (var i = 0; i < count; i++) {
                    list.add(elementCodec.decode(buf));
                }
                return list;
            }
        };
    }

    // </editor-fold>

    public static <T> StreamCodec<T> fromCodec(Codec<T> codec) {
        return fromCodec(codec, () -> new NbtAccounter(2097152L));
    }

    public static <T> StreamCodec<T> fromCodec(Codec<T> codec, Supplier<NbtAccounter> supplier) {
        return tag(supplier).map(o -> codec.encodeStart(NbtOps.INSTANCE, o).getOrThrow(false, s -> LOGGER.error("Failed to encode: {} {}", s, o)),
                tag -> codec.parse(NbtOps.INSTANCE, tag).getOrThrow(false, s -> LOGGER.error("Failed to decode: {} {}", s, tag)));
    }
}
