package io.github.tt432.eyelib.molang.platform.mapping;

import io.github.tt432.eyelib.molang.mapping.api.MolangMappingTree;
//? if <1.20.6 {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
//?}
/**
 * 映射树生命周期钩子（Forge 通用设置）。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
//?}
public final class MolangMappingTreeLifecycleHooks {
    private MolangMappingTreeLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        MolangMappingTree.setupMolangMappingTree(new ForgeMolangMappingDiscovery());
    }
}
