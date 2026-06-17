package io.github.tt432.eyelib.molang.platform.compiler;

import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/**
 * 编译生命周期钩子（Forge 事件）。
 *
 * @author TT432
 */
@Mod.EventBusSubscriber
public final class MolangCompileLifecycleHooks {
    private MolangCompileLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
    }
}