package io.github.tt432.eyelib.importer.animation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** 命名轨道的异构容器。
 * @author TT432 */
@NullMarked
public interface NamedTrackContainerDefinition<N, T extends io.github.tt432.eyelib.importer.animation.NamedTrackDefinition<N>> {
    Map<N, T> tracksByName();

    default @Nullable T trackOrNull(N name) {
        return tracksByName().get(name);
    }
}