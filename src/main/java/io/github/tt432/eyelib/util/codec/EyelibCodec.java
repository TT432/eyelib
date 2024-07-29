package io.github.tt432.eyelib.util.codec;

import com.mojang.serialization.Codec;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibCodec {
    public static final Codec<Float> STR_FLOAT_CODEC =
            Codec.withAlternative(Codec.FLOAT, Codec.STRING.xmap(Float::parseFloat, String::valueOf));
}