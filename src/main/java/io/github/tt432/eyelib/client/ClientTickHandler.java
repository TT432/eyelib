package io.github.tt432.eyelib.client;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * @author TT432
 */
@EventBusSubscriber(Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientTickHandler {
    @Getter
    static int tick;

    @SubscribeEvent
    public static void onEvent(ClientTickEvent.Pre event) {
        if (!Minecraft.getInstance().isPaused())
            tick++;
    }
}
