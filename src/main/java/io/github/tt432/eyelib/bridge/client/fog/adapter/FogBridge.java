package io.github.tt432.eyelib.bridge.client.fog.adapter;

import io.github.tt432.eyelib.importer.addon.BrFog;
import io.github.tt432.eyelib.importer.addon.FogAssetRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;

import java.util.Optional;
//? if <26.1 {
import net.minecraft.client.renderer.FogRenderer;
//?} else {
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.AtmosphericFogEnvironment;
//?}
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
//?}

/**
 * bedrock fog 渲染桥：订阅各版本 {@code ViewportEvent.RenderFog / ComputeFogColor}，
 * 把 {@link FogAssetRegistry#activeFog()} 的 distance.air 应用到主渲染雾。
 * 无激活 fog（或无 air 子项）时早退，完全不碰 vanilla 雾态。
 * <p>
 * 版本差异（事件签名以各版本 sources 实证）：
 * <ul>
 *   <li>&lt;26.1：RenderFog 可取消且仅一对 near/far；render 型比例先换算为块距
 *       （值 × effectiveRenderDistance × 16）后写入，并必须 {@code setCanceled(true)} 才生效。
 *       仅在 FOG_TERRAIN（天空雾不动）且 FogType 为空气/大气时生效（水下/岩浆雾不碰）；</li>
 *   <li>26.1.2：RenderFog 不可取消，直接改写可变 {@link FogData}——environmental 与
 *       renderDistance 两段同值写入（着色器取球面/圆柱两段雾的 max，同写方可完整表达
 *       Bedrock 的 fixed/render 两种语义）；仅 AtmosphericFogEnvironment（空气/大气）生效；</li>
 *   <li>颜色三版本同走 ComputeFogColor（仅相机不在流体中时覆盖）。</li>
 * </ul>
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class FogBridge {
    private FogBridge() {
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        //? if <26.1 {
        // 仅覆盖地形雾（FOG_SKY 天空雾不动），且仅空气/大气介质
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;
        // 仅空气介质（<26.1 FogType 枚举无 ATMOSPHERIC 项，空气即相机不在流体中的 NONE）
        if (event.getType() != FogType.NONE) return;
        //?} else {
        // 26.1.2：仅大气雾环境（水/岩浆/细雪/黑暗等其他 FogEnvironment 不动）
        if (!(event.getEnvironment() instanceof AtmosphericFogEnvironment)) return;
        //?}

        BrFog.DistanceSetting air = activeAirDistance().orElse(null);
        if (air == null) return;

        float renderDistanceBlocks = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0f;
        float start = air.fogStartBlocks(renderDistanceBlocks);
        float end = air.fogEndBlocks(renderDistanceBlocks);

        //? if <26.1 {
        event.setNearPlaneDistance(start);
        event.setFarPlaneDistance(end);
        // <26.1 必须取消事件，上面的距离修改才会被 FogRenderer 采用
        event.setCanceled(true);
        //?} else {
        FogData fogData = event.getFogData();
        fogData.environmentalStart = start;
        fogData.environmentalEnd = end;
        fogData.renderDistanceStart = start;
        fogData.renderDistanceEnd = end;
        //?}
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        // 仅空气介质（相机不在任何流体中）；水下/岩浆雾色不碰
        if (event.getCamera().getFluidInCamera() != FogType.NONE) return;

        BrFog.DistanceSetting air = activeAirDistance().orElse(null);
        if (air == null) return;

        float[] rgb = air.rgbFloats();
        event.setRed(rgb[0]);
        event.setGreen(rgb[1]);
        event.setBlue(rgb[2]);
    }

    /** 当前激活 fog 的 distance.air；无激活 fog 或无 air 子项时为空（早退信号）。 */
    private static Optional<BrFog.DistanceSetting> activeAirDistance() {
        return FogAssetRegistry.activeFog()
                .map(fog -> fog.distance(BrFog.MEDIUM_AIR));
    }
}
