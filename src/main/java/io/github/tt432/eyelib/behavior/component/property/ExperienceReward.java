package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * minecraft:experience_reward — 实体经验奖励属性。
 * Bedrock 规范: { "on_death": string, "on_bred": string (optional) }
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record ExperienceReward(
        String on_death,
        Optional<String> on_bred
) implements io.github.tt432.eyelib.behavior.component.Component {
    public static final Codec<ExperienceReward> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("on_death", "0").forGetter(ExperienceReward::on_death),
            Codec.STRING.optionalFieldOf("on_bred").forGetter(ExperienceReward::on_bred)
    ).apply(ins, ExperienceReward::new));

    @Override
    public String id() {
        return "experience_reward";
    }
}
