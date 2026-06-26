package io.github.tt432.eyelib.bridge.ui;

import io.github.tt432.eyelib.ui.UIScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/**
 * Screen 开放 Port：application 通过 {@link #register} 注册 UIScreen 工厂，
 * bridge 事件订阅类（如 ManagerScreenOpenEvents）通过 {@link #open} 打开屏幕，
 * 无需直接引用 application 层的 Screen 实现类。
 *
 * @author TT432
 */
public final class ScreenPort {
    private ScreenPort() {}

    private static Supplier<UIScreen> managerScreenSupplier = () -> null;

    public static void register(Supplier<UIScreen> supplier) {
        managerScreenSupplier = supplier;
    }

    public static void openManagerScreen() {
        UIScreen screen = managerScreenSupplier.get();
        if (screen != null) {
            net.minecraft.client.Minecraft.getInstance().setScreen(MCScreenAdapter.wrap(screen));
        }
    }

    public static Screen wrap(UIScreen screen) {
        return MCScreenAdapter.wrap(screen);
    }
}
