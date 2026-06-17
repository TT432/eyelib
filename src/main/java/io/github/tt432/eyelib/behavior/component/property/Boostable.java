package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:boostable
 *
 * @param boost_items       list of boost items
 * @param duration          boost duration (default 3.0f)
 * @param speed_multiplier  speed multiplier (default 1.0f)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Boostable(
        List<BoostItem> boost_items,
        float duration,
        float speed_multiplier
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Boostable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BoostItem.CODEC.listOf().fieldOf("boost_items").forGetter(Boostable::boost_items),
            Codec.FLOAT.optionalFieldOf("duration", 3.0f).forGetter(Boostable::duration),
            Codec.FLOAT.optionalFieldOf("speed_multiplier", 1.0f).forGetter(Boostable::speed_multiplier)
    ).apply(inst, Boostable::new));

    @Override
    public String id() {
        return "boostable";
    }

    public record BoostItem(String item, String replace_item, int damage) {
        static final Codec<BoostItem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("item").forGetter(BoostItem::item),
                Codec.STRING.fieldOf("replace_item").forGetter(BoostItem::replace_item),
                Codec.INT.optionalFieldOf("damage", 1).forGetter(BoostItem::damage)
        ).apply(inst, BoostItem::new));
    }
}
