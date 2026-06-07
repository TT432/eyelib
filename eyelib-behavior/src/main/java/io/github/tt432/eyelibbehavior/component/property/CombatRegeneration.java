package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibbehavior.component.Component;
import org.jspecify.annotations.NullMarked;

/**
 * minecraft:combat_regeneration — 战斗状态下的生命恢复。
 *
 * @author TT432
 */
@NullMarked
public record CombatRegeneration(
        boolean can_regenerate,
        int regeneration_delay,
        int regeneration_amount,
        float regeneration_exhaustion
) implements Component {
    public static final Codec<CombatRegeneration> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.BOOL.optionalFieldOf("can_regenerate", true).forGetter(CombatRegeneration::can_regenerate),
            Codec.INT.optionalFieldOf("regeneration_delay", 60).forGetter(CombatRegeneration::regeneration_delay),
            Codec.INT.optionalFieldOf("regeneration_amount", 1).forGetter(CombatRegeneration::regeneration_amount),
            Codec.FLOAT.optionalFieldOf("regeneration_exhaustion", 0.0f).forGetter(CombatRegeneration::regeneration_exhaustion)
    ).apply(ins, CombatRegeneration::new));

    @Override
    public String id() {
        return "combat_regeneration";
    }
}
