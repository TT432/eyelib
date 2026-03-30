package io.github.tt432.eyelib.client.render.controller;

import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RenderControllerLookup {
    @Nullable
    public static RenderControllerEntry get(String name) {
        return RenderControllerManager.INSTANCE.get(name);
    }
}
