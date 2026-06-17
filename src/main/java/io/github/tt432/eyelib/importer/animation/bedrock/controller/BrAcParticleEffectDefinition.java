package io.github.tt432.eyelib.importer.animation.bedrock.controller;

import io.github.tt432.eyelib.molang.MolangValue;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

/** @author TT432 */
@NullMarked
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
