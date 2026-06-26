package io.github.tt432.eyelib.bridge.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

/**
 * 监听客户端 tick，按 V 键打开 ModelPreviewScreen。
 * 通过 {@link #openScreenSupplier} 注入 Screen 工厂，避免 bridge 直接依赖 application 的 Screen 类。
 *
 * @author TT432
 */
public final class ModelPreviewScreenHook {
    private ModelPreviewScreenHook() {}

    /**
     * Screen 工厂，由 application 层 ClientBootstrap 注入；默认 no-op。
     */
    public static Supplier<net.minecraft.client.gui.screens.Screen> openScreenSupplier = () -> null;

    //? if <1.20.6 {
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class GameBusHandlers {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onEvent(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            tryOpen();
        }
    }
    //?} else {
    @net.neoforged.fml.common.EventBusSubscriber(net.neoforged.api.distmarker.Dist.CLIENT)
    public static final class GameBusHandlers {
        //? if <26.1 {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onEvent(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
        //?} else {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onEvent(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
        //?}
            tryOpen();
        }
    }
    //?}

    private static void tryOpen() {
        //? if <1.20.6 {
        if (net.minecraftforge.fml.loading.FMLLoader.isProduction()) return;
        //?} elif <26.1 {
        if (net.neoforged.fml.loading.FMLLoader.isProduction()) return;
        //?} else {
        if (false) return;
        //?}
        if (Minecraft.getInstance().screen == null
                && //? if <26.1 {
                InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_V)
                //?} else {
                false
                //?}
        ) {
            var screen = openScreenSupplier.get();
            if (screen != null) {
                Minecraft.getInstance().setScreen(screen);
            }
        }
    }
}
