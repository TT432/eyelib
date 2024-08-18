package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.Optional;

/**
 * @author TT432
 */
public record BrEffectsKeyFrame(
        float timestamp,
        String effect,
        Optional<String> locator,
        Optional<MolangValue> preEffectScript
) {
    public record Factory(
            String effect,
            Optional<String> locator,
            Optional<MolangValue> preEffectScript
    ) {
        public static final Codec<Factory> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("effect").forGetter(o -> o.effect),
                Codec.STRING.optionalFieldOf("locator").forGetter(o -> o.locator),
                MolangValue.CODEC.optionalFieldOf("pre_effect_script").forGetter(o -> o.preEffectScript)
        ).apply(ins, Factory::new));

        public BrEffectsKeyFrame to(float timestamp) {
            return new BrEffectsKeyFrame(timestamp, effect, locator, preEffectScript);
        }

        public static Factory from(BrEffectsKeyFrame keyFrame) {
            return new Factory(keyFrame.effect(), keyFrame.locator(), keyFrame.preEffectScript);
        }
    }
}
