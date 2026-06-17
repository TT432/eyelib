package io.github.tt432.eyelibbehavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * minecraft:on_death — 实体死亡事件触发器。
 * Bedrock 规范: { "event": string, "target": string }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record OnDeath(
        String event,
        String target
) implements io.github.tt432.eyelibbehavior.component.Component {
    public static final Codec<OnDeath> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("event").forGetter(OnDeath::event),
            Codec.STRING.optionalFieldOf("target", "self").forGetter(OnDeath::target)
    ).apply(ins, OnDeath::new));

    @Override
    public String id() {
        return "on_death";
    }
}
