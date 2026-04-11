package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;
import io.github.tt432.eyelib.core.util.codec.Eithers;

public class EitherHelper {
    public static <U> U unwrap(final Either<? extends U, ? extends U> either) {
        return Eithers.unwrap(either);
    }
}
