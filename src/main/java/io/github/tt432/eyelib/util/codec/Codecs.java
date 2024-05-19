package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Codecs {
    public static <S> S unwrap(Either<S, S> either) {
        return either.map(Function.identity(), Function.identity());
    }
}
