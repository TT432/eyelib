package io.github.tt432.eyelib.bridge.client.gui.manager;

import io.github.tt432.eyelib.bridge.ForgeEnvironment;
import io.github.tt432.eyelib.bridge.ui.ScreenPort;
import io.github.tt432.eyelib.ui.UIScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

/**
 * 监听客户端 tick，按 G 键打开动画曲线视图。
 *
 * @author TT432
 */
public final class AnimationViewHook {
    private AnimationViewHook() {}

    public static Supplier<UIScreen> openScreenSupplier = () -> null;

    //? if <1.20.6 {
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class GameBusHandlers {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onEvent(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
                return;
            }
            tryOpen();
        }
    }
    //?} else {
    @net.neoforged.fml.common.EventBusSubscriber(net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class GameBusHandlers {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onEvent(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            tryOpen();
        }
    }
    //?}

    private static void tryOpen() {
        if (ForgeEnvironment.isProduction() || Minecraft.getInstance().level == null || Minecraft.getInstance().screen != null) {
            return;
        }

        if (//? if <26.1 {
                GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_G) == GLFW.GLFW_PRESS
                //?} else {
                false
                //?}
        ) {
            UIScreen screen = openScreenSupplier.get();
            if (screen != null) {
                Minecraft.getInstance().setScreen(ScreenPort.wrap(screen));
            }
        }
    }
}
