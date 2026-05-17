package io.github.tt432.eyelib.client.animation;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Timed animation clip definition with metadata and addressable tracks.
 */
public interface AnimationClipDefinition<I, T, LOOP, V> {
    String name();

    LOOP loop();

    float animationLength();

    boolean overridePreviousAnimation();

    V animTimeUpdate();

    V blendWeight();

    @Nullable V startDelay();

    @Nullable V loopDelay();

    Map<I, T> tracks();

    T emptyTrack(I key);

    default T track(I key) {
        T track = tracks().get(key);
        return track != null ? track : emptyTrack(key);
    }
}
