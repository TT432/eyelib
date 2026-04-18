package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.NamedTrackContainerDefinition;
import io.github.tt432.eyelibmolang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.util.EnumMap;
import java.util.Map;

public record BrAnimationEntryTracksDefinition(
        BrAnimationEntryEffectTrackDefinition<BrEffectsKeyFrameDefinition> soundEffects,
        BrAnimationEntryEffectTrackDefinition<BrEffectsKeyFrameDefinition> particleEffects,
        BrAnimationEntryEffectTrackDefinition<MolangValue> timeline,
        BrAnimationEntryBoneTrackDefinition bones
) implements NamedTrackContainerDefinition<BrAnimationEntryTrackName, BrAnimationEntryTrackDefinition> {
    public static BrAnimationEntryTracksDefinition of(
            AnimationEffect<BrEffectsKeyFrameDefinition> soundEffects,
            AnimationEffect<BrEffectsKeyFrameDefinition> particleEffects,
            AnimationEffect<MolangValue> timeline,
            Int2ObjectMap<BrBoneAnimation> bones
    ) {
        return new BrAnimationEntryTracksDefinition(
                new BrAnimationEntryEffectTrackDefinition<>(BrAnimationEntryTrackName.SOUND_EFFECTS, soundEffects),
                new BrAnimationEntryEffectTrackDefinition<>(BrAnimationEntryTrackName.PARTICLE_EFFECTS, particleEffects),
                new BrAnimationEntryEffectTrackDefinition<>(BrAnimationEntryTrackName.TIMELINE, timeline),
                new BrAnimationEntryBoneTrackDefinition(BrAnimationEntryTrackName.BONES, bones)
        );
    }

    @Override
    public Map<BrAnimationEntryTrackName, BrAnimationEntryTrackDefinition> tracksByName() {
        EnumMap<BrAnimationEntryTrackName, BrAnimationEntryTrackDefinition> result =
                new EnumMap<>(BrAnimationEntryTrackName.class);
        result.put(soundEffects.name(), soundEffects);
        result.put(particleEffects.name(), particleEffects);
        result.put(timeline.name(), timeline);
        result.put(bones.name(), bones);
        return Map.copyOf(result);
    }

    public Map<BrAnimationEntryTrackName, BrAnimationEntryTrackDefinition> byName() {
        return tracksByName();
    }
}
