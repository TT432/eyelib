package io.github.tt432.eyelib.client.animation;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Heterogeneous container of named tracks.
 */
public interface NamedTrackContainerDefinition<N, T extends NamedTrackDefinition<N>> {
    Map<N, T> tracksByName();

    default @Nullable T trackOrNull(N name) {
        return tracksByName().get(name);
    }
}
