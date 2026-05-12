package io.github.tt432.eyelibimporter.animation;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Heterogeneous container of named tracks.
 */
public interface NamedTrackContainerDefinition<N, T extends io.github.tt432.eyelibimporter.animation.NamedTrackDefinition<N>> {
    Map<N, T> tracksByName();

    default @Nullable T trackOrNull(N name) {
        return tracksByName().get(name);
    }
}
