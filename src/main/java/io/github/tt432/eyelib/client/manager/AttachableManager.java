package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;

/** @author TT432 */
public final class AttachableManager {
    public static final Registry<BrClientEntity> INSTANCE =
            new Registry<>("AttachableManager", ManagerEventPublishBridge::publishManagerEntryChanged);

    private AttachableManager() {
    }
}
