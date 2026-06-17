package io.github.tt432.eyelib.molang.platform.mapping;

import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jspecify.annotations.NullMarked;

/**
 * 映射树生命周期钩子（Forge 通用设置）。
 *
 * @author TT432
 */
@NullMarked
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MolangMappingTreeLifecycleHooks {
    private MolangMappingTreeLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        MolangMappingTree.setupMolangMappingTree(new ForgeMolangMappingDiscovery());
    }
}