package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelib.client.animation.Animation;
import io.github.tt432.eyelib.client.animation.RuntimeParticlePlayData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BrControllerStateOwner {
    private float startTick = -1;
    @Nullable private BrAcStateDefinition lastState;
    @Nullable private BrAcStateDefinition currState;
    private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();
    private Map<String, String> currentAnimations = new Object2ObjectOpenHashMap<>();
    private final List<RuntimeParticlePlayData> particles = new ArrayList<>();

    @SuppressWarnings("unchecked")
    <D> D getData(Animation<?> animation) {
        return (D) data.computeIfAbsent(animation.identityPort().name(), s -> animation.statePort().createData());
    }

    float startTick() { return startTick; }
    void startTick(float startTick) { this.startTick = startTick; }
    @Nullable BrAcStateDefinition lastState() { return lastState; }
    void lastState(@Nullable BrAcStateDefinition lastState) { this.lastState = lastState; }
    @Nullable BrAcStateDefinition currState() { return currState; }
    void currState(@Nullable BrAcStateDefinition currState) { this.currState = currState; }
    Map<String, String> currentAnimations() { return currentAnimations; }
    void currentAnimations(Map<String, String> currentAnimations) { this.currentAnimations = currentAnimations; }
    List<RuntimeParticlePlayData> particles() { return particles; }
}

