package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.behavior.BehaviorEntity;
import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;

/** @author TT432 */
public final class BehaviorEntityManager {
    public static final Registry<BehaviorEntity> INSTANCE =
            new Registry<>("BehaviorEntityManager", ManagerEventPublishBridge::publishManagerEntryChanged);

    private BehaviorEntityManager() {
    }
}
