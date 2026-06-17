package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;

/**
 * minecraft:navigation.walk
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record NavigationWalk(
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
    public static final Codec<NavigationWalk> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("avoid_water", false).forGetter(NavigationWalk::avoid_water),
            Codec.BOOL.optionalFieldOf("avoid_portals", false).forGetter(NavigationWalk::avoid_portals),
            Codec.BOOL.optionalFieldOf("can_break_doors", false).forGetter(NavigationWalk::can_break_doors),
            Codec.BOOL.optionalFieldOf("can_open_doors", false).forGetter(NavigationWalk::can_open_doors),
            Codec.BOOL.optionalFieldOf("can_path_over_water", false).forGetter(NavigationWalk::can_path_over_water),
            Codec.BOOL.optionalFieldOf("can_path_over_lava", false).forGetter(NavigationWalk::can_path_over_lava),
            Codec.BOOL.optionalFieldOf("can_sink", true).forGetter(NavigationWalk::can_sink),
            Codec.BOOL.optionalFieldOf("can_pass_doors", true).forGetter(NavigationWalk::can_pass_doors),
            Codec.BOOL.optionalFieldOf("can_swim", false).forGetter(NavigationWalk::can_swim),
            Codec.BOOL.optionalFieldOf("can_walk", true).forGetter(NavigationWalk::can_walk),
            Codec.BOOL.optionalFieldOf("can_jump", true).forGetter(NavigationWalk::can_jump),
            Codec.BOOL.optionalFieldOf("can_breach", false).forGetter(NavigationWalk::can_breach)
    ).apply(ins, NavigationWalk::new));

    @Override
    public String id() {
        return "navigation.walk";
    }
}
