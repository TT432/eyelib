package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.molang.MolangValue;

import java.util.Optional;

/**
 * @author TT432
 */
public record BrAcParticleEffect(
        Optional<String> effect,
        Optional<String> locator,
        boolean bindToActor,
        MolangValue preEffectScript
) {
    public static final Codec<BrAcParticleEffect> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.optionalFieldOf("effect").forGetter(o -> o.effect),
            Codec.STRING.optionalFieldOf("locator").forGetter(o -> o.locator),
            Codec.BOOL.optionalFieldOf("bind_to_actor", false).forGetter(o -> o.bindToActor),
            MolangValue.CODEC.optionalFieldOf("pre_effect_script", MolangValue.ZERO).forGetter(o -> o.preEffectScript)
    ).apply(ins, BrAcParticleEffect::new));
}
