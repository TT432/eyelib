package io.github.tt432.eyelib.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.TickEvent;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientTickHandler {
    static int tick;
    static float pTick;

    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().isPaused())
            tick++;
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START && !Minecraft.getInstance().isPaused())
            pTick = event.renderTickTime;
    }

    public static float getTick() {
        return tick + pTick;
    }
}
