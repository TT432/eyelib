package io.github.tt432.eyelibmolang.platform.compiler;

import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.NullMarked;

/**
 * 编译生命周期钩子（Forge 事件）。
 *
 * @author TT432
 */
@NullMarked
@Mod.EventBusSubscriber
public final class MolangCompileLifecycleHooks {
    private MolangCompileLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
    }
}