package io.github.tt432.eyelib.bridge.client.dnd;

import io.github.tt432.eyelib.bridge.client.dnd.adapter.DragDropManager;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
//?}

/**
 * 拖放管理器安装钩子（ManagerEventLifecycleHooks 同款模式）：
 * FMLClientSetupEvent（window 已创建）→ {@link DragDropManager#install()}。
 * 失败 fail-soft（OLE 不可用时退回 MC 原生 onFilesDrop）。
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} elif <26.1 {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class DragDropLifecycleHooks {
    private DragDropLifecycleHooks() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // enqueueWork：window 创建在 setup 前完成，但 OLE 注册走主线程安全路径
        event.enqueueWork(DragDropManager.INSTANCE::install);
    }
}
