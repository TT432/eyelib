package io.github.tt432.eyelib.client;

import io.github.tt432.eyelib.sound.SoundManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib3.resource.GeckoLibCache;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceListener {
	@SubscribeEvent
	public static void onEvent(RegisterClientReloadListenersEvent event) {
		event.registerReloadListener(GeckoLibCache.getInstance()::reload);
		event.registerReloadListener(SoundManager.getInstance()::reload);
	}
}
