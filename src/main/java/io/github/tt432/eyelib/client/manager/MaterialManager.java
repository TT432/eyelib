package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;

/** @author TT432 */
public final class MaterialManager {
    public static final Registry<BrMaterialEntry> INSTANCE =
            new Registry<>("MaterialManager", ManagerEventPublishBridge::publishManagerEntryChanged);

    private MaterialManager() {
    }
}
