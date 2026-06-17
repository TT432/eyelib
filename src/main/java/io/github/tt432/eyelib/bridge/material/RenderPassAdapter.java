package io.github.tt432.eyelib.bridge.material;

import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.RenderType;
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
        return switch (pass.transparency()) {
            case SOLID -> RenderType.entitySolid(ResourceLocationBridge.toMc(texture));
            case ALPHA_TEST -> pass.disableCulling()
                    ? RenderType.entityCutoutNoCull(ResourceLocationBridge.toMc(texture))
                    : RenderType.entityCutout(ResourceLocationBridge.toMc(texture));
            case TRANSLUCENT -> pass.disableCulling()
                    ? RenderType.entityTranslucent(ResourceLocationBridge.toMc(texture))
                    : RenderType.entityTranslucentCull(ResourceLocationBridge.toMc(texture));
            case TRANSLUCENT_EMISSIVE -> RenderType.entityTranslucentEmissive(ResourceLocationBridge.toMc(texture));
            case ADDITIVE -> RenderType.entityTranslucent(ResourceLocationBridge.toMc(texture));
        };
    }
}
