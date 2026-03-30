package io.github.tt432.eyelib.client.gui.manager.hotkey;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class ManagerScreenKeybinds {
    public static final KeyMapping OPEN_SCREEN = new KeyMapping("Open Eyelib Manager Screen", GLFW.GLFW_KEY_I, "Eyelib");

    @SubscribeEvent
    public static void onEvent(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN);
    }
}
