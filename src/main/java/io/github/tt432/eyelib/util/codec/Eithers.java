package io.github.tt432.eyelibutil.codec;

import com.mojang.datafixers.util.Either;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * Either 工具类，提供统一的解包操作。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Eithers {
    public static <U> U unwrap(final Either<? extends U, ? extends U> either) {
        return either.map(Function.identity(), Function.identity());
    }
}