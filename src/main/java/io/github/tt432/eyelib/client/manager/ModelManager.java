package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;

/** @author TT432 */
public final class ModelManager {
    public static final Registry<Model> INSTANCE =
            new Registry<>("ModelManager", ManagerEventPublishBridge::publishManagerEntryChanged);

    private ModelManager() {
    }
}
