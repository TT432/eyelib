package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;
import java.util.function.Function;

/**
 * @author TT432 <br/>
 * Copy from <a href="https://github.com/TT432/chin/blob/main/src/main/java/io/github/tt432/chin/codec/ChinExtraCodecs.java">tt432/chin</a><br/>
 * As you wish!
 */
public class ChinExtraCodecs {
    /**
     * Example: <br/>
     * var codec = singleOrList(Codec.INT); <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("1")); // return List(1) <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("[1, 2]")); // return List(1, 2) <br/>
     */
    public static <A> Codec<List<A>> singleOrList(Codec<A> codec) {
        return Codec.either(codec.xmap(List::of, l -> l.get(0)), codec.listOf()).xmap(EitherHelper::unwrap, Either::right);
    }

    /**
     * Example: <br/>
     * var codec = check(Codec.STRING, s -> s.equals("1.23") ? DataResult.success(s) : DataResult.error(() -> "Not equals 1.23")); <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("\"1.23\"")); // return "1.23" <br/>
     * codec.parse(JsonOps.INSTANCE, gson.fromJson("\"2.23\"")); // throw <br/>
     *
     * @see Codec#intRange(int, int)
     */
    public static <A> Codec<A> check(Codec<A> sourceCodec, Function<A, DataResult<A>> checker) {
        return sourceCodec.flatXmap(checker, checker);
    }
}
