package io.github.tt432.eyelib.util.streamcodec;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 提供基于 FriendlyByteBuf 的常见类型流编解码常量与方法。
 *
 * @author TT432
 */
public class EyelibStreamCodecs {
    private static final Logger LOGGER = LogUtils.getLogger();

    // <editor-fold desc="Primitive types">

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<Boolean> BOOL = io.github.tt432.eyelib.util.streamcodec.StreamCodec.create((obj, buf) -> buf.writeBoolean(obj), FriendlyByteBuf::readBoolean);

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<Float> FLOAT = io.github.tt432.eyelib.util.streamcodec.StreamCodec.create((obj, buf) -> buf.writeFloat(obj), FriendlyByteBuf::readFloat);

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<Double> DOUBLE = io.github.tt432.eyelib.util.streamcodec.StreamCodec.create((obj, buf) -> buf.writeDouble(obj), FriendlyByteBuf::readDouble);

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<String> STRING = io.github.tt432.eyelib.util.streamcodec.StreamCodec.create((obj, buf) -> buf.writeUtf(obj), FriendlyByteBuf::readUtf);

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<Integer> VAR_INT = io.github.tt432.eyelib.util.streamcodec.StreamCodec.create((obj, buf) -> buf.writeVarInt(obj), FriendlyByteBuf::readVarInt);

    // </editor-fold>

    // <editor-fold desc="Minecraft types">

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<PortResourceLocation> PORT_RESOURCE_LOCATION =
            io.github.tt432.eyelib.util.streamcodec.StreamCodec.create(
                    (obj, buf) -> STRING.encode(obj.toString(), buf),
                    buf -> PortResourceLocation.parse(STRING.decode(buf)));

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<Vector3f> VECTOR_3_F = new io.github.tt432.eyelib.util.streamcodec.StreamCodec<>() {
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

    public static io.github.tt432.eyelib.util.streamcodec.StreamCodec<Tag> tag(final Supplier<NbtAccounter> supplier) {
        return new io.github.tt432.eyelib.util.streamcodec.StreamCodec<>() {
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

    public static final io.github.tt432.eyelib.util.streamcodec.StreamCodec<CompoundTag> COMPOUND_TAG = new io.github.tt432.eyelib.util.streamcodec.StreamCodec<CompoundTag>() {
        @Override
        public void encode(CompoundTag obj, FriendlyByteBuf buf) {
            buf.writeNbt(obj);
        }

        @Override
        public CompoundTag decode(FriendlyByteBuf buf) {
            return buf.readNbt();
        }
    };

    public static <K, V> io.github.tt432.eyelib.util.streamcodec.StreamCodec<Map<K, V>> map(Supplier<Map<K, V>> factory, io.github.tt432.eyelib.util.streamcodec.StreamCodec<K> keyCodec, io.github.tt432.eyelib.util.streamcodec.StreamCodec<V> valueCodec) {
        return new io.github.tt432.eyelib.util.streamcodec.StreamCodec<>() {
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

    public static <E> io.github.tt432.eyelib.util.streamcodec.StreamCodec<List<E>> list(Supplier<List<E>> factory, io.github.tt432.eyelib.util.streamcodec.StreamCodec<E> elementCodec) {
        return new io.github.tt432.eyelib.util.streamcodec.StreamCodec<>() {
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

    public static <T> io.github.tt432.eyelib.util.streamcodec.StreamCodec<T> fromCodec(Codec<T> codec) {
        //? if <1.20.6 {
        return fromCodec(codec, () -> new NbtAccounter(2097152L));
        //?} else {
        return fromCodec(codec, () -> new NbtAccounter(2097152L, 0));
        //?}
    }

    public static <T> io.github.tt432.eyelib.util.streamcodec.StreamCodec<T> fromCodec(Codec<T> codec, Supplier<NbtAccounter> supplier) {
        return tag(supplier).map(o -> codec.encodeStart(NbtOps.INSTANCE, o)
                        //? if <1.20.6 {
                        .getOrThrow(false, s -> LOGGER.error("Failed to encode: {} {}", s, o)),
                        //?} else {
                        .getOrThrow(s -> {
                            LOGGER.error("Failed to encode: {} {}", s, o);
                            return new RuntimeException(s);
                        }),
                        //?}
                tag -> codec.parse(NbtOps.INSTANCE, tag)
                        //? if <1.20.6 {
                        .getOrThrow(false, s -> LOGGER.error("Failed to decode: {} {}", s, tag)));
                        //?} else {
                        .getOrThrow(s -> {
                            LOGGER.error("Failed to decode: {} {}", s, tag);
                            return new RuntimeException(s);
                        }));
                        //?}
    }
}
