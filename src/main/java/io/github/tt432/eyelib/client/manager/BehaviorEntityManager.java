package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelibbehavior.BehaviorEntity;
import io.github.tt432.eyelibutil.manager.ManagerReadPort;
import io.github.tt432.eyelibutil.manager.ManagerWritePort;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public class BehaviorEntityManager extends Manager<BehaviorEntity> {
    public static final BehaviorEntityManager INSTANCE = new BehaviorEntityManager();

    public static ManagerReadPort<BehaviorEntity> readPort() {
        return INSTANCE;
    }

    public static ManagerWritePort<BehaviorEntity> writePort() {
        return INSTANCE;
    }
}
