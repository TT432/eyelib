package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.util.event.api.RenderStageRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
//?}

/**
 * 渲染阶段事件适配器：在每帧「实体渲染之前、且早于光影着色器的 shadow pass」的时机，
 * 通过 {@link RenderStageRegistries#renderStage()} 分发给 {@code @OnRenderStage} 标记的方法。
 *
 * <p><b>为什么不是 AFTER_SKY</b>：Iris/Oculus 的 shadow pass 在 {@code LevelRenderer.renderLevel}
 * 早期（sky 之前）就把实体几何写进 shadow map，而 {@code RenderLevelStageEvent.AFTER_SKY} 在其之后。
 * eyelib 的实体姿态由本适配器每帧计算一次（{@code EntityRenderOrchestrator} 的 setup/tick 管道），
 * 若管道在 AFTER_SKY 才跑，shadow pass 会使用上一帧的姿态：主渲染可见面落在上一帧轮廓的阴影里，
 * 模型整片发黑（2026-11-03 MakeUp/Iris 实证；含自带遮挡道具的模型如流浪商人伞架会完全黑掉）。
 * 因此管道必须早于 shadow pass 执行，同时晚于客户端 tick（姿态/实体位置为当前帧）。
 *
 * <ul>
 *   <li>{@code <1.20.6}（Forge）：{@link TickEvent.RenderTickEvent} START，在
 *       {@code Minecraft.runTick} 调 {@code GameRenderer.render} 之前触发。</li>
 *   <li>{@code >=1.20.6}（NeoForge）：{@link RenderFrameEvent.Pre}，语义相同。</li>
 * </ul>
 *
 * <p>注意：此时 {@code GameRenderer.renderLevel} 尚未执行 {@code Camera.setup}，
 * {@link Minecraft#gameRenderer} 的主相机位置是上一帧的（本适配器只把它用于
 * {@code shouldRender} 距离剔除；动画姿态本身不依赖相机位置，相机查询类 molang 允许一帧滞后）。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class RenderStageEventAdapter {
    private RenderStageEventAdapter() {
    }

    //? if <1.20.6 {
    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        RenderStageRegistries.renderStage().dispatch(event.renderTickTime, position.x, position.y, position.z);
    }
    //?} else {
    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        //? if <26.1 {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        //?} else {
        Vec3 position = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        //?}
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        RenderStageRegistries.renderStage().dispatch(partialTick, position.x, position.y, position.z);
    }
    //?}
}
