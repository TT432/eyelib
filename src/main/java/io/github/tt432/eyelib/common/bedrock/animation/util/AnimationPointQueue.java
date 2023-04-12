/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.common.bedrock.animation.util;

import com.mojang.math.Vector3d;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.util.LinkedList;

/**
 * An animation point queue holds a queue of Animation Points which are used in
 * the AnimatedEntityModel to lerp between values
 */
public class AnimationPointQueue extends LinkedList<AnimationPointQueue.LerpInfo> {

    @Serial
    private static final long serialVersionUID = 5472797438476621193L;

    @RequiredArgsConstructor
    @Data
    public static class LerpInfo {
        final Vector3d value;

        @Override
        public String toString() {
            return value != null ? "LerpInfo{" +
                    " x: " + value.x +
                    " y: " + value.y +
                    " z: " + value.z +
                    '}' : "LerpInfo{value=null}";
        }
    }
}
