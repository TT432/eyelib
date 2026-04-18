package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelibmolang.MolangValue;

import java.util.List;
import java.util.Map;

public record BrAcStateDefinition(
        MolangValue onEntry,
        MolangValue onExit,
        float blendTransition,
        boolean blendViaShortestPath,
        BrAcStateTracksDefinition namedTracks
) {
    public static BrAcStateDefinition fromSchema(BrAcState schema) {
        return new BrAcStateDefinition(
                schema.onEntry(),
                schema.onExit(),
                schema.blendTransition(),
                schema.blendViaShortestPath(),
                BrAcStateTracksDefinition.of(
                        schema.animations(),
                        schema.particleEffects().stream().map(BrAcParticleEffectDefinition::fromSchema).toList(),
                        schema.soundEffects(),
                        schema.transitions()
                )
        );
    }

    public BrAcState toSchema() {
        return new BrAcState(
                animations(),
                onEntry,
                onExit,
                particleEffects().stream().map(BrAcParticleEffectDefinition::toSchema).toList(),
                soundEffects(),
                transitions(),
                blendTransition,
                blendViaShortestPath
        );
    }

    public Map<String, MolangValue> animations() {
        return namedTracks.animations().animations();
    }

    public List<BrAcParticleEffectDefinition> particleEffects() {
        return namedTracks.particleEffects().particleEffects();
    }

    public List<String> soundEffects() {
        return namedTracks.soundEffects().soundEffects();
    }

    public Map<String, MolangValue> transitions() {
        return namedTracks.transitions().transitions();
    }
}
