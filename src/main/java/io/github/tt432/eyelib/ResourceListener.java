package io.github.tt432.eyelib;

import io.github.tt432.eyelib.common.bedrock.BedrockResourceManager;
import io.github.tt432.eyelib.common.sound.EyelibSoundManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceListener {
    @SubscribeEvent
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(BedrockResourceManager.getInstance()::reload);
        event.registerReloadListener(EyelibSoundManager.getInstance()::reload);
    }
}
