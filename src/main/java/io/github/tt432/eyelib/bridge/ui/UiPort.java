package io.github.tt432.eyelib.bridge.ui;

import io.github.tt432.eyelib.bridge.ui.adapter.ScreenPort;
import io.github.tt432.eyelib.ui.UIScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/**
 * UI Port：application 通过此接口注册和包装 UIScreen，避免直接依赖 bridge.adapter 包。
 */
public interface UiPort {
    static void register(Supplier<UIScreen> supplier) {
        ScreenPort.register(supplier);
    }

    static Screen wrap(UIScreen screen) {
        return ScreenPort.wrap(screen);
    }
}
