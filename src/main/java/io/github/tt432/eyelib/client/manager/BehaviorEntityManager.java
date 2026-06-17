package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.behavior.BehaviorEntity;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public class BehaviorEntityManager extends Manager<BehaviorEntity> {
    public static final BehaviorEntityManager INSTANCE = new BehaviorEntityManager();
}
