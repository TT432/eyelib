package io.github.tt432.eyelib.client.render.controller;

import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

/** @author TT432 */
@NullMarked
public final class RenderControllerLookup {
    @Nullable
    public static RenderControllerEntry get(String name) {
        return RenderControllerManager.readPort().get(name);
    }
}
