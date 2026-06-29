package io.github.tt432.eyelib.bridge.client.gui.manager.hotkey;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.KeyMapping;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
//?}
import org.lwjgl.glfw.GLFW;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?}
public final class ManagerScreenKeybinds {
    //? if <26.1 {
    public static final KeyMapping OPEN_SCREEN = new KeyMapping("Open Eyelib Manager Screen", GLFW.GLFW_KEY_I, "Eyelib");
    //?} else {
    public static final KeyMapping OPEN_SCREEN = new KeyMapping("Open Eyelib Manager Screen", GLFW.GLFW_KEY_I, KeyMapping.Category.MISC);
    //?}

    @SubscribeEvent
    public static void onEvent(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN);
    }
}
