package io.github.tt432.eyelib.client.animation.bedrock;

import io.github.tt432.eyelib.client.animation.AnimationEffect;
import io.github.tt432.eyelib.client.animation.RuntimeParticlePlayData;
import io.github.tt432.eyelib.client.particle.ParticleSpawnService;
import io.github.tt432.eyelibmolang.MolangValue;

import java.util.ArrayList;
import java.util.List;

final class BrClipStateOwner {
    private final BrAnimationPlaybackState playbackState = new BrAnimationPlaybackState();
    private final List<AnimationEffect.Runtime<?>> effects = new ArrayList<>();
    private final List<RuntimeParticlePlayData> particles = new ArrayList<>();

    private int loopedTimes;
    private float lastTicks;
    private float animTime;
    private float deltaTime;

    BrClipStateOwner resetEffects(AnimationEffect<BrEffectsKeyFrameDefinition> soundEffects,
                                  AnimationEffect<BrEffectsKeyFrameDefinition> particleEffects,
                                  AnimationEffect<MolangValue> timeline) {
        effects.clear();
        effects.add(soundEffects.runtime());
        effects.add(particleEffects.runtime());
        effects.add(timeline.runtime());
        return this;
    }

    void syncStateFields() {
        loopedTimes = playbackState.loopedTimes();
        lastTicks = playbackState.lastTicks();
        animTime = playbackState.animTime();
        deltaTime = playbackState.deltaTime();
    }

    void finish(AnimationEffect<BrEffectsKeyFrameDefinition> soundEffects,
                AnimationEffect<BrEffectsKeyFrameDefinition> particleEffects,
                AnimationEffect<MolangValue> timeline) {
        playbackState.reset();
        syncStateFields();
        resetEffects(soundEffects, particleEffects, timeline);

        for (var particle : particles) {
            ParticleSpawnService.removeEmitter(particle.particleUUID());
        }
        particles.clear();
    }

    BrAnimationPlaybackState playbackState() { return playbackState; }
    List<AnimationEffect.Runtime<?>> effects() { return effects; }
    List<RuntimeParticlePlayData> particles() { return particles; }
    int loopedTimes() { return loopedTimes; }
    float lastTicks() { return lastTicks; }
    float animTime() { return animTime; }
    float deltaTime() { return deltaTime; }
}
