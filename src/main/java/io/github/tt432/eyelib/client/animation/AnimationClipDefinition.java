package io.github.tt432.eyelib.client.animation;

import org.jspecify.annotations.Nullable;

/**
 * Timed animation clip definition with metadata and addressable tracks.
 */
public interface AnimationClipDefinition<I, T, LOOP, V> extends TrackAnimationDefinition<I, T> {
    String name();

    LOOP loop();

    float animationLength();

    boolean overridePreviousAnimation();

    V animTimeUpdate();

    V blendWeight();

    @Nullable V startDelay();

    @Nullable V loopDelay();
}
