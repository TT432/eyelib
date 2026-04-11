package io.github.tt432.eyelib.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientTickHandler {
    static int tick;

    public static int getTick() {
        return tick;
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event) {
        if (!Minecraft.getInstance().isPaused() && event.phase == TickEvent.Phase.START)
            tick++;
    }
}
