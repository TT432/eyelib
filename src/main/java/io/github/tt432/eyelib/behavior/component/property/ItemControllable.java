package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:item_controllable
 *
 * @param control_items list of control items (empty by default)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ItemControllable(List<String> control_items) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<ItemControllable> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().optionalFieldOf("control_items", List.of()).forGetter(ItemControllable::control_items)
    ).apply(inst, ItemControllable::new));

    @Override
    public String id() {
        return "item_controllable";
    }
}
