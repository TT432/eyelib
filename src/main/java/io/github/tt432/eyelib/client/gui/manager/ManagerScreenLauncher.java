package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.bridge.ui.UiPort;
import net.minecraft.client.Minecraft;

/**
 * 管理器屏幕之间的导航入口，通过 bridge ScreenPort 打开 MC Screen。
 *
 * @author TT432
 */
public final class ManagerScreenLauncher {
    private ManagerScreenLauncher() {}

    public static void openEntitiesScreen() {
        Minecraft.getInstance().setScreen(UiPort.wrap(new EntitiesScreen()));
    }
}

