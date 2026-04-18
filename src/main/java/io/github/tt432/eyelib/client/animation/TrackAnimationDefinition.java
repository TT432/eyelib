package io.github.tt432.eyelib.client.animation;

import java.util.Map;

/**
 * Immutable animation definition composed from addressable tracks.
 */
public interface TrackAnimationDefinition<I, T> {
    Map<I, T> tracks();

    T emptyTrack(I key);

    default T track(I key) {
        T track = tracks().get(key);
        return track != null ? track : emptyTrack(key);
    }
}
