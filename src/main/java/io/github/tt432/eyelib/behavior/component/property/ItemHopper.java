package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:item_hopper — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ItemHopper() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final ItemHopper INSTANCE = new ItemHopper();

    public static final Codec<ItemHopper> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "item_hopper";
    }
}
