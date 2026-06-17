package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:dweller — 居民组件，控制实体在村庄等定居点的行为。
 *
 * @author TT432
 */
@NullMarked
public record Dweller(
        String dwelling_type,
        String dweller_role,
        float update_interval_base,
        float update_interval_variant,
        boolean can_find_poi,
        boolean can_migrate,
        int first_founding_reward
) implements Component {
    public static final Codec<Dweller> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("dwelling_type", "village").forGetter(Dweller::dwelling_type),
            Codec.STRING.optionalFieldOf("dweller_role", "inhabitant").forGetter(Dweller::dweller_role),
            Codec.FLOAT.optionalFieldOf("update_interval_base", 60.0f).forGetter(Dweller::update_interval_base),
            Codec.FLOAT.optionalFieldOf("update_interval_variant", 40.0f).forGetter(Dweller::update_interval_variant),
            Codec.BOOL.optionalFieldOf("can_find_poi", false).forGetter(Dweller::can_find_poi),
            Codec.BOOL.optionalFieldOf("can_migrate", true).forGetter(Dweller::can_migrate),
            Codec.INT.optionalFieldOf("first_founding_reward", 0).forGetter(Dweller::first_founding_reward)
    ).apply(ins, Dweller::new));

    @Override
    public String id() {
        return "dweller";
    }
}
