package io.github.tt432.eyelib.bridge.material;

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
        return switch (pass.transparency()) {
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
