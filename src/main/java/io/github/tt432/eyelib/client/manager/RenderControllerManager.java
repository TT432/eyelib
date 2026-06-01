package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelibutil.manager.ManagerReadPort;
import io.github.tt432.eyelibutil.manager.ManagerWritePort;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public class RenderControllerManager extends Manager<RenderControllerEntry> {
    public static final RenderControllerManager INSTANCE = new RenderControllerManager();

    public static ManagerReadPort<RenderControllerEntry> readPort() {
        return INSTANCE;
    }

    public static ManagerWritePort<RenderControllerEntry> writePort() {
        return INSTANCE;
    }
}