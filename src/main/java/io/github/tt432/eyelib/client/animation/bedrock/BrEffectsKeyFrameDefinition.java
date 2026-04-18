package io.github.tt432.eyelib.client.animation.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.animation.AnimationKeyframeDefinition;
import io.github.tt432.eyelibimporter.animation.bedrock.BrEffectsKeyFrame;
import io.github.tt432.eyelibmolang.MolangValue;

import java.util.Optional;

public record BrEffectsKeyFrameDefinition(
        float timestamp,
        String effect,
        Optional<String> locator,
        Optional<MolangValue> preEffectScript
) implements AnimationKeyframeDefinition {
    public static BrEffectsKeyFrameDefinition fromSchema(BrEffectsKeyFrame schema) {
        return new BrEffectsKeyFrameDefinition(schema.timestamp(), schema.effect(), schema.locator(), schema.preEffectScript());
    }

    public BrEffectsKeyFrame toSchema() {
        return new BrEffectsKeyFrame(timestamp, effect, locator, preEffectScript);
    }

    public record Factory(
            String effect,
            Optional<String> locator,
            Optional<MolangValue> preEffectScript
    ) {
        public static final Codec<Factory> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("effect").forGetter(Factory::effect),
                Codec.STRING.optionalFieldOf("locator").forGetter(Factory::locator),
                MolangValue.CODEC.optionalFieldOf("pre_effect_script").forGetter(Factory::preEffectScript)
        ).apply(ins, Factory::new));

        public BrEffectsKeyFrameDefinition to(float timestamp) {
            return new BrEffectsKeyFrameDefinition(timestamp, effect, locator, preEffectScript);
        }

        public static Factory from(BrEffectsKeyFrameDefinition keyFrame) {
            return new Factory(keyFrame.effect(), keyFrame.locator(), keyFrame.preEffectScript());
        }
    }
}
