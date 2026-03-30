package io.github.tt432.eyelib.client.gui.manager.hotkey;

import io.github.tt432.eyelib.client.gui.manager.EyelibManagerScreen;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class ManagerScreenOpenEvents {
    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event) {
        if (ManagerScreenKeybinds.OPEN_SCREEN.isDown() && Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(EyelibManagerScreen.create());
        }
    }
}
