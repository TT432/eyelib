package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

public class CodecHelper {
    public static <T> Codec<T> withAlternative(final Codec<T> primary, final Codec<? extends T> alternative) {
        return Codec.either(primary, alternative).xmap(EitherHelper::unwrap, Either::left);
    }
}
