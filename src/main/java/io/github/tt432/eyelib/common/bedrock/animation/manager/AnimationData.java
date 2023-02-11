/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.common.bedrock.animation.manager;

import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.common.bedrock.animation.AnimationController;
import io.github.tt432.eyelib.util.BoneSnapshot;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

@Data
public class AnimationData {
    @Getter
    private final Map<String, Object> extraData = new HashMap<>();

    private final Map<String, Pair<Bone, BoneSnapshot>> boneSnapshotCollection;
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
     * Instantiates a new Animation controller collection.
     */
    public AnimationData() {
        super();
        boneSnapshotCollection = new Object2ObjectOpenHashMap<>();
    }

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
