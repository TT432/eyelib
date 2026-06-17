package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.molang.mapping.api.MolangQueryRuntimeBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
/**
 * 查询运行时生命周期钩子（Forge 客户端设置）。
 *
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MolangQueryRuntimeLifecycleHooks {
    private MolangQueryRuntimeLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MolangQueryRuntimeBridge.install(new MinecraftMolangQueryRuntime());
    }
}