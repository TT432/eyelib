/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.common.bedrock.animation.manager;

import io.github.tt432.eyelib.common.bedrock.animation.AnimationController;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class AnimationData {
    public static final AnimationData EMPTY = new AnimationData();

    @Getter
    private final Map<String, Object> extraData = new HashMap<>();

    @SuppressWarnings("all")
    public <T> Optional<T> getExtraData(String name) {
        if (extraData.containsKey(name)) {
            return Optional.ofNullable((T) extraData.get(name));
        }

        return Optional.empty();
    }

    @SuppressWarnings("all")
    public <T> T getExtraData(String key, T defaultValue) {
        return (T) extraData.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("all")
    public <T> T getOrCreateExtraData(String key, T defaultValue) {
        return (T) extraData.computeIfAbsent(key, s -> defaultValue);
    }

    public void putExtraData(String key, Object value) {
        extraData.put(key, value);
    }

    public void removeExtraData(String s) {
        extraData.remove(s);
    }

    private final Map<String, AnimationController> animationControllers = new Object2ObjectOpenHashMap<>();

    private double tick;
    private boolean firstTick = true;
    /**
     * This is how long it takes for any bones that don't have an animation to
     * revert back to their original position
     */
    private double resetTickLength = 1;
    private double startTick = -1;
    private boolean shouldPlayWhilePaused = false;

    /**
     * This method is how you register animation controllers, without this, your
     * AnimationPredicate method will never be called
     *
     * @param value The value
     * @return the animation controller
     */
    public AnimationController addAnimationController(AnimationController value) {
        return this.animationControllers.put(value.getName(), value);
    }
}
