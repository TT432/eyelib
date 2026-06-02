package io.github.tt432.eyelibmolang.platform.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangQueryRuntimeBridge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jspecify.annotations.NullMarked;

/**
 * 查询运行时生命周期钩子（Forge 客户端设置）。
 *
 * @author TT432
 */
@NullMarked
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MolangQueryRuntimeLifecycleHooks {
    private MolangQueryRuntimeLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MolangQueryRuntimeBridge.install(new MinecraftMolangQueryRuntime());
    }
}