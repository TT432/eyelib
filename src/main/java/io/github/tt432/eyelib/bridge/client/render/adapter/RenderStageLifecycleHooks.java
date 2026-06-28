package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.util.event.api.RenderStageRegistries;
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
 * 渲染阶段订阅者生命周期钩子（Forge 通用设置），对齐 {@code MolangMappingTreeLifecycleHooks} 范式。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(modid = "eyelib")
//?}
public final class RenderStageLifecycleHooks {
    private RenderStageLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        RenderStageRegistries.setupRenderStage(new ForgeRenderStageSubscriberDiscovery());
    }
}
