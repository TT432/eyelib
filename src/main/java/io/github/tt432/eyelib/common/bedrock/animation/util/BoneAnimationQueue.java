/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.common.bedrock.animation.util;

import io.github.tt432.eyelib.api.bedrock.model.Bone;

public record BoneAnimationQueue(Bone bone,
                                 AnimationPointQueue rotate,
                                 AnimationPointQueue position,
                                 AnimationPointQueue scale) {

    public BoneAnimationQueue(Bone bone) {
        this(bone, new AnimationPointQueue(), new AnimationPointQueue(), new AnimationPointQueue());
    }
}
