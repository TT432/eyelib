package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;

import java.util.function.Function;

public class EitherHelper {
    public static <U> U unwrap(final Either<? extends U, ? extends U> either) {
        return either.map(Function.identity(), Function.identity());
    }
}
