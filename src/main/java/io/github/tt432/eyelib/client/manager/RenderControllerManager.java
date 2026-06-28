package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;

/** @author TT432 */
public final class RenderControllerManager {
    public static final Registry<RenderControllerEntry> INSTANCE =
            new Registry<>("RenderControllerManager", ManagerEventPublishBridge::publishManagerEntryChanged);

    private RenderControllerManager() {
    }
}
