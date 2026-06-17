package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * minecraft:home
 *
 * @param home_block_list     list of home block types (empty by default)
 * @param restriction_radius  restriction radius (default -1)
 * @param use_home_position   whether to use home position (default false)
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record Home(
        List<String> home_block_list,
        int restriction_radius,
        boolean use_home_position
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<Home> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.listOf().optionalFieldOf("home_block_list", List.of()).forGetter(Home::home_block_list),
            Codec.INT.optionalFieldOf("restriction_radius", -1).forGetter(Home::restriction_radius),
            Codec.BOOL.optionalFieldOf("use_home_position", false).forGetter(Home::use_home_position)
    ).apply(inst, Home::new));

    @Override
    public String id() {
        return "home";
    }
}
