package io.github.tt432.eyelibmolang;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;
import java.util.function.Function;

final class MolangCodecs {
    private MolangCodecs() {
    }

    private static final Codec<String> SCALAR_STRING_CODEC = Codec.either(Codec.STRING, Codec.either(Codec.FLOAT, Codec.BOOL))
            .xmap(either -> either.map(Function.identity(), scalar -> scalar.map(Object::toString, value -> value ? "1" : "0")), Either::left);

    static Codec<List<String>> singleOrListStrings() {
        return Codec.either(SCALAR_STRING_CODEC.xmap(List::of, values -> values.get(0)), SCALAR_STRING_CODEC.listOf())
                .xmap(either -> either.map(Function.identity(), Function.identity()), Either::right);
    }

    static <T> Codec<List<T>> fixedSizeList(Codec<T> elementCodec, int size) {
        return elementCodec.listOf().comapFlatMap(
                values -> values.size() == size
                        ? DataResult.success(values)
                        : DataResult.error(() -> "Expected list size " + size + " but got " + values.size()),
                Function.identity()
        );
    }
}
