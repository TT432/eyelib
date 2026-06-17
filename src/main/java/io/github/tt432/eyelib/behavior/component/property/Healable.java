package io.github.tt432.eyelibbehavior.component.property;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:healable
 *
 * @param items     list of heal items
 * @param force_use whether force use (default false)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Healable(
        List<HealItem> items,
        boolean force_use
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<Healable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            HealItem.CODEC.listOf().fieldOf("items").forGetter(Healable::items),
            Codec.BOOL.optionalFieldOf("force_use", false).forGetter(Healable::force_use)
    ).apply(inst, Healable::new));

    @Override
    public String id() {
        return "healable";
    }

    public record HealItem(String item, int heal_amount, JsonObject filters) {
        static final Codec<HealItem> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("item").forGetter(HealItem::item),
                Codec.INT.optionalFieldOf("heal_amount", 1).forGetter(HealItem::heal_amount),
                Codec.STRING.xmap(
                        JsonParser::parseString,
                        JsonElement::toString
                ).xmap(
                        e -> e.getAsJsonObject(),
                        o -> o
                ).optionalFieldOf("filters", new JsonObject()).forGetter(HealItem::filters)
        ).apply(inst, HealItem::new));
    }
}
