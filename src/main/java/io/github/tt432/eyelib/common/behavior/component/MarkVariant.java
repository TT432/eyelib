package io.github.tt432.eyelib.common.behavior.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * @author TT432
 */
public record MarkVariant(
        int value
) implements Component {
    public static final Codec<MarkVariant> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("value").forGetter(MarkVariant::value)
    ).apply(ins, MarkVariant::new));

    @Override
    public String id() {
        return "mark_variant";
    }
}
