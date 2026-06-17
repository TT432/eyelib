package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/**
 * @author TT432
 */
@NullMarked
public record Breathable(
        int total_supply,
        int suffocate_time,
        boolean breathes_air,
        boolean breathes_water,
        boolean breathes_lava,
        boolean breathes_solids,
        boolean generates_bubbles,
        float inhale_time,
        Optional<List<String>> breathe_blocks,
        Optional<List<String>> non_breathe_blocks
) implements Component {
    public static final Codec<Breathable> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.optionalFieldOf("total_supply", 15).forGetter(Breathable::total_supply),
            Codec.INT.optionalFieldOf("suffocate_time", -20).forGetter(Breathable::suffocate_time),
            Codec.BOOL.optionalFieldOf("breathes_air", true).forGetter(Breathable::breathes_air),
            Codec.BOOL.optionalFieldOf("breathes_water", false).forGetter(Breathable::breathes_water),
            Codec.BOOL.optionalFieldOf("breathes_lava", true).forGetter(Breathable::breathes_lava),
            Codec.BOOL.optionalFieldOf("breathes_solids", false).forGetter(Breathable::breathes_solids),
            Codec.BOOL.optionalFieldOf("generates_bubbles", true).forGetter(Breathable::generates_bubbles),
            Codec.FLOAT.optionalFieldOf("inhale_time", 0.0f).forGetter(Breathable::inhale_time),
            Codec.STRING.listOf().optionalFieldOf("breathe_blocks").forGetter(Breathable::breathe_blocks),
            Codec.STRING.listOf().optionalFieldOf("non_breathe_blocks").forGetter(Breathable::non_breathe_blocks)
    ).apply(ins, Breathable::new));

    @Override
    public String id() {
        return "breathable";
    }
}
