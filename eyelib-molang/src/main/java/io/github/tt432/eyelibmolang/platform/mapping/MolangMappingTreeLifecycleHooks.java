package io.github.tt432.eyelibmolang.platform.mapping;

import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
/** @author TT432 */
public final class MolangMappingTreeLifecycleHooks {
    private MolangMappingTreeLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        MolangMappingTree.setupMolangMappingTree(new ForgeMolangMappingDiscovery());
    }
}