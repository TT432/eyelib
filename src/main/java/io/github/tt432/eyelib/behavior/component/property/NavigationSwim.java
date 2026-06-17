package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * minecraft:navigation.swim
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationSwim(
        boolean avoid_water,
        boolean avoid_portals,
        boolean can_break_doors,
        boolean can_open_doors,
        boolean can_path_over_water,
        boolean can_path_over_lava,
        boolean can_sink,
        boolean can_pass_doors,
        boolean can_swim,
        boolean can_walk,
        boolean can_jump,
        boolean can_breach
) implements Component {
    public static final Codec<NavigationSwim> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("avoid_water", false).forGetter(NavigationSwim::avoid_water),
            Codec.BOOL.optionalFieldOf("avoid_portals", false).forGetter(NavigationSwim::avoid_portals),
            Codec.BOOL.optionalFieldOf("can_break_doors", false).forGetter(NavigationSwim::can_break_doors),
            Codec.BOOL.optionalFieldOf("can_open_doors", false).forGetter(NavigationSwim::can_open_doors),
            Codec.BOOL.optionalFieldOf("can_path_over_water", false).forGetter(NavigationSwim::can_path_over_water),
            Codec.BOOL.optionalFieldOf("can_path_over_lava", false).forGetter(NavigationSwim::can_path_over_lava),
            Codec.BOOL.optionalFieldOf("can_sink", true).forGetter(NavigationSwim::can_sink),
            Codec.BOOL.optionalFieldOf("can_pass_doors", true).forGetter(NavigationSwim::can_pass_doors),
            Codec.BOOL.optionalFieldOf("can_swim", true).forGetter(NavigationSwim::can_swim),
            Codec.BOOL.optionalFieldOf("can_walk", false).forGetter(NavigationSwim::can_walk),
            Codec.BOOL.optionalFieldOf("can_jump", true).forGetter(NavigationSwim::can_jump),
            Codec.BOOL.optionalFieldOf("can_breach", true).forGetter(NavigationSwim::can_breach)
    ).apply(ins, NavigationSwim::new));

    @Override
    public String id() {
        return "navigation.swim";
    }
}
