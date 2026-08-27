//? if <26.1 {
package io.github.tt432.eyelib.bridge.client.render.skinning.adapter;

//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
//?}

/**
 * C1 跨实体合批的 level 渲染窗口 hook（&le;26.1，ADR-0032）：
 * AFTER_SKY 开窗（实体相开始），AFTER_ENTITIES drain + 关窗（实体相结束、半透明地形之前），
 * AFTER_LEVEL 安全 drain + 关窗。stage 不触发的环境（nether 无 sky 假设/GUI/手动 FBO 渲染）
 * 窗口保持关闭，{@link BatchSkinningDispatcher#submit} 走立即 drain 回退。
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class SkinningBatchStageHook {
    private SkinningBatchStageHook() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!LegacySkinningManager.batchingActive()) {
            return;
        }
        // Stage 在 Forge/NeoForge 均为可扩展 class（非 enum），用引用比较
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            BatchSkinningDispatcher.openWindow();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            BatchSkinningDispatcher.drain();
            BatchSkinningDispatcher.closeWindow();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            BatchSkinningDispatcher.drain();
            BatchSkinningDispatcher.closeWindow();
        }
    }
}
//?}
