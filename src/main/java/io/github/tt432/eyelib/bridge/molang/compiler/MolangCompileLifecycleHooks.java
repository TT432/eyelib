package io.github.tt432.eyelib.bridge.molang.compiler;

//? if <1.20.6 {
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
//?}
/**
 * 编译生命周期钩子（Forge 事件）。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber
//?} else {
@EventBusSubscriber
//?}
public final class MolangCompileLifecycleHooks {
    private MolangCompileLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onGameShutdown(GameShuttingDownEvent event) {
    }
}
