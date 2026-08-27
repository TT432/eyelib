package io.github.tt432.eyelib.bridge.material.adapter;

import io.github.tt432.eyelib.bridge.material.ResourceLocationBridge;
import io.github.tt432.eyelib.bridge.material.BridgeRenderPass;
//? if <26.1
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningManager;

import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?}
/**
 * 将 PortRenderPass 语义转换为 MC RenderType 实例。
 *
 * @author TT432
 */
public final class RenderPassAdapter {
    private RenderPassAdapter() {}


    public static RenderType toRenderType(PortRenderPass pass, PortResourceLocation texture) {
        if (pass instanceof BridgeRenderPass bridgePass) {
            return bridgePass.renderType();
        }
        //? if <26.1 {
        // C1 GPU 蒙皮（ADR-0032）：默认状态材质走 vanilla RenderType（非 custom），
        // 也必须登记蒙皮变体，否则绝大多数实体静默回退 CPU 蒙皮（2026-08-28 实证：A&S 全实体 variants=0）。
        // 变体选择与 RenderPassAdapter 的 vanilla shader 语义一一对应（蒙皮 shader 是其逐行复刻）。
        RenderType legacy = switch (pass.transparency()) {
            case SOLID -> RenderType.entitySolid(ResourceLocationBridge.toMc(texture));
            case ALPHA_TEST -> {
                if (pass.disableCulling()) {
                    yield RenderType.entityCutoutNoCull(ResourceLocationBridge.toMc(texture));
                }
                yield RenderType.entityCutout(ResourceLocationBridge.toMc(texture));
            }
            case TRANSLUCENT -> {
                if (pass.disableCulling()) {
                    yield RenderType.entityTranslucent(ResourceLocationBridge.toMc(texture));
                }
                yield RenderType.entityTranslucentCull(ResourceLocationBridge.toMc(texture));
            }
            case TRANSLUCENT_EMISSIVE -> RenderType.entityTranslucentEmissive(ResourceLocationBridge.toMc(texture));
            case ADDITIVE -> RenderType.entityTranslucent(ResourceLocationBridge.toMc(texture));
        };
        LegacySkinningManager.registerVariant(legacy, switch (pass.transparency()) {
            case SOLID -> LegacySkinningManager.VARIANT_SOLID;
            case ALPHA_TEST -> LegacySkinningManager.VARIANT_CUTOUT;
            case TRANSLUCENT, ADDITIVE -> LegacySkinningManager.VARIANT_TRANSLUCENT;
            case TRANSLUCENT_EMISSIVE -> LegacySkinningManager.VARIANT_EMISSIVE;
        });
        return legacy;
        //?} else {
        return switch (pass.transparency()) {
            case SOLID -> RenderTypes.entitySolid(ResourceLocationBridge.toMc(texture));
            case ALPHA_TEST -> pass.disableCulling()
                    ? RenderTypes.entityCutout(ResourceLocationBridge.toMc(texture))
                    : RenderTypes.entityCutoutCull(ResourceLocationBridge.toMc(texture));
            case TRANSLUCENT -> RenderTypes.entityTranslucent(ResourceLocationBridge.toMc(texture));
            case TRANSLUCENT_EMISSIVE -> RenderTypes.entityTranslucentEmissive(ResourceLocationBridge.toMc(texture));
            case ADDITIVE -> RenderTypes.entityTranslucent(ResourceLocationBridge.toMc(texture));
        };
        //?}
    }
}


