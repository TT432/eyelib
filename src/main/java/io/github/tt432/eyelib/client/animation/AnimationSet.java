package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelib.client.animation.bedrock.BrAnimation;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public record AnimationSet(
        Map<String, Animation<?>> animations
) {
    public static final AnimationSet EMPTY = new AnimationSet(new HashMap<>());

    public static AnimationSet from(BrAnimation animation) {
        return new AnimationSet(new HashMap<>(animation.animations()));
    }
}
