package io.github.tt432.eyelib.mc.impl.molang.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MolangMappingTreeLifecycleHooks {
    private MolangMappingTreeLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        MolangMappingTree.setupMolangMappingTree(new ForgeMolangMappingDiscovery());
    }
}
