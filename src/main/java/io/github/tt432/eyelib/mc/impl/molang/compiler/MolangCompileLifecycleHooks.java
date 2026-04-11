package io.github.tt432.eyelib.mc.impl.molang.compiler;

import io.github.tt432.eyelibmolang.compiler.MolangCompileHandler;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class MolangCompileLifecycleHooks {
    private MolangCompileLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
        MolangCompileHandler.shutdown();
    }
}
