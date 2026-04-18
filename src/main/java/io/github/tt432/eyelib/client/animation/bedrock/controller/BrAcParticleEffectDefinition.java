package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcParticleEffect;
import io.github.tt432.eyelibmolang.MolangValue;

import java.util.Optional;

public record BrAcParticleEffectDefinition(
        Optional<String> effect,
        Optional<String> locator,
        boolean bindToActor,
        MolangValue preEffectScript
) {
    public static BrAcParticleEffectDefinition fromSchema(BrAcParticleEffect schema) {
        return new BrAcParticleEffectDefinition(schema.effect(), schema.locator(), schema.bindToActor(), schema.preEffectScript());
    }

    public BrAcParticleEffect toSchema() {
        return new BrAcParticleEffect(effect, locator, bindToActor, preEffectScript);
    }
}
