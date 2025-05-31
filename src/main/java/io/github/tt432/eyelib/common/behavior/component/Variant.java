package io.github.tt432.eyelib.common.behavior.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * @author TT432
 */
public record Variant(
        int value
) implements Component {

    public static final Codec<Variant> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("value").forGetter(Variant::value)
    ).apply(ins, Variant::new));

    @Override
    public String id() {
        return "variant";
    }
}
