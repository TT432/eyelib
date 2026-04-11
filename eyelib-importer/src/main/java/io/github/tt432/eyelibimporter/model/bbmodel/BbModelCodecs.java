package io.github.tt432.eyelibimporter.model.bbmodel;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class BbModelCodecs {
    private BbModelCodecs() {
    }

    static final Codec<Vector2f> FLOATS2VEC2F_CODEC = Codec.FLOAT.listOf().xmap(
            l -> new Vector2f(l.get(0), l.get(1)),
            v -> List.of(v.x, v.y)
    );

    static final Codec<Vector3f> FLOATS2VEC3F_CODEC = Codec.FLOAT.listOf().xmap(
            l -> new Vector3f(l.get(0), l.get(1), l.get(2)),
            v -> List.of(v.x, v.y, v.z)
    );

    static final Codec<Vector4f> FLOATS2VEC4F_CODEC = Codec.FLOAT.listOf().xmap(
            l -> new Vector4f(l.get(0), l.get(1), l.get(2), l.get(3)),
            v -> List.of(v.x, v.y, v.z, v.w)
    );

    static <T> Codec<T> withAlternative(Codec<T> primary, Codec<? extends T> alternative) {
        return Codec.either(primary, alternative).xmap(e -> e.map(Function.identity(), Function.identity()), Either::left);
    }

    static <A> MapCodec<Optional<A>> optionalMapCodec(MapCodec<A> mapCodec) {
        return new MapCodec<>() {
            @Override
            public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return input.map(value -> mapCodec.encode(value, ops, prefix)).orElse(prefix);
            }

            @Override
            public <T> java.util.stream.Stream<T> keys(DynamicOps<T> ops) {
                return mapCodec.keys(ops);
            }

            @Override
            public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
                return DataResult.success(mapCodec.decode(ops, input).result());
            }
        };
    }

    static <A> Codec<A> recursive(String name, Function<Codec<A>, Codec<A>> wrapped) {
        return new RecursiveCodec<>(name, wrapped);
    }

    private static final class RecursiveCodec<T> implements Codec<T> {
        private final String name;
        private final Function<Codec<T>, Codec<T>> wrapped;
        private Codec<T> delegate;

        private RecursiveCodec(String name, Function<Codec<T>, Codec<T>> wrapped) {
            this.name = name;
            this.wrapped = wrapped;
        }

        private Codec<T> delegate() {
            Codec<T> local = delegate;
            if (local == null) {
                synchronized (this) {
                    local = delegate;
                    if (local == null) {
                        local = wrapped.apply(this);
                        delegate = local;
                    }
                }
            }
            return local;
        }

        @Override
        public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> ops, S input) {
            return delegate().decode(ops, input);
        }

        @Override
        public <S> DataResult<S> encode(T input, DynamicOps<S> ops, S prefix) {
            return delegate().encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "RecursiveCodec[" + name + ']';
        }
    }
}
