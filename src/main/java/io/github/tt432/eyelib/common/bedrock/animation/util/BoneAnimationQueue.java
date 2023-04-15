/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.common.bedrock.animation.util;


import io.github.tt432.eyelib.common.bedrock.model.element.Bone;

import java.util.LinkedList;

public record BoneAnimationQueue(Bone bone,
                                 LinkedList<LerpInfo> rotate,
                                 LinkedList<LerpInfo> position,
                                 LinkedList<LerpInfo> scale) {

    public BoneAnimationQueue(Bone bone) {
        this(bone, new LinkedList<>(), new LinkedList<>(), new LinkedList<>());
    }
}
