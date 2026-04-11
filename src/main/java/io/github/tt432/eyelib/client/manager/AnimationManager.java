package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.animation.Animation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnimationManager extends Manager<Animation<?>> {
    public static final AnimationManager INSTANCE = new AnimationManager();

    public static ManagerReadPort<Animation<?>> readPort() {
        return INSTANCE;
    }

    public static ManagerWritePort<Animation<?>> writePort() {
        return INSTANCE;
    }
}
