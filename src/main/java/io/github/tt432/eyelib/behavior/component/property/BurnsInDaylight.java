package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;

/**
 * minecraft:burns_in_daylight — marker component.
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record BurnsInDaylight() implements io.github.tt432.eyelib.behavior.component.Component {
    private static final BurnsInDaylight INSTANCE = new BurnsInDaylight();

    public static final Codec<BurnsInDaylight> CODEC = Codec.unit(INSTANCE);

    @Override
    public String id() {
        return "burns_in_daylight";
    }
}
