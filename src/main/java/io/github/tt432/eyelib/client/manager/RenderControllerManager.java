package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderControllerManager extends Manager<RenderControllerEntry> {
    public static final RenderControllerManager INSTANCE = new RenderControllerManager();
}
