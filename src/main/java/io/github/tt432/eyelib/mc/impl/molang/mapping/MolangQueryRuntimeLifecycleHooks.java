package io.github.tt432.eyelib.mc.impl.molang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangQueryRuntimeBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MolangQueryRuntimeLifecycleHooks {
    private MolangQueryRuntimeLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MolangQueryRuntimeBridge.install(new MinecraftMolangQueryRuntime());
    }
}
