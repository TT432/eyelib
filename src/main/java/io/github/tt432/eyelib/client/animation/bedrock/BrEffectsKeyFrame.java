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
    public static final Codec<BrEffectsKeyFrame> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.fieldOf("timestamp").forGetter(o -> o.timestamp),
            Codec.STRING.fieldOf("effect").forGetter(o -> o.effect),
            Codec.STRING.optionalFieldOf("locator").forGetter(o -> o.locator),
            MolangValue.CODEC.optionalFieldOf("preEffectScript").forGetter(o -> o.preEffectScript)
    ).apply(ins, BrEffectsKeyFrame::new));
}
