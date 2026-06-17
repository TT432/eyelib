package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:teleport — 传送组件，控制实体的瞬移行为。
 *
 * @author TT432
 */
@NullMarked
public record Teleport(
        float dark_teleport_chance,
        float light_teleport_chance,
        boolean random_teleports,
        float target_teleport_chance,
        float max_random_teleport_time,
        float min_random_teleport_time,
        float max_target_teleport_time,
        float min_target_teleport_time
) implements Component {
    public static final Codec<Teleport> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("dark_teleport_chance", 0.01f).forGetter(Teleport::dark_teleport_chance),
            Codec.FLOAT.optionalFieldOf("light_teleport_chance", 0.01f).forGetter(Teleport::light_teleport_chance),
            Codec.BOOL.optionalFieldOf("random_teleports", false).forGetter(Teleport::random_teleports),
            Codec.FLOAT.optionalFieldOf("target_teleport_chance", 1.0f).forGetter(Teleport::target_teleport_chance),
            Codec.FLOAT.optionalFieldOf("max_random_teleport_time", 20.0f).forGetter(Teleport::max_random_teleport_time),
            Codec.FLOAT.optionalFieldOf("min_random_teleport_time", 0.0f).forGetter(Teleport::min_random_teleport_time),
            Codec.FLOAT.optionalFieldOf("max_target_teleport_time", 20.0f).forGetter(Teleport::max_target_teleport_time),
            Codec.FLOAT.optionalFieldOf("min_target_teleport_time", 0.0f).forGetter(Teleport::min_target_teleport_time)
    ).apply(ins, Teleport::new));

    @Override
    public String id() {
        return "teleport";
    }
}
