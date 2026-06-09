package io.github.tt432.eyelibbridge.material;

import io.github.tt432.eyelibmaterial.port.PortRenderPass;
import io.github.tt432.eyelibutil.PortResourceLocation;
import net.minecraft.client.renderer.RenderType;
import org.jspecify.annotations.NullMarked;

/**
 * 将 PortRenderPass 语义转换为 MC RenderType 实例。
 *
 * @author TT432
 */
@NullMarked
public final class RenderPassAdapter {

    private RenderPassAdapter() {}

    public static RenderType toRenderType(PortRenderPass pass, PortResourceLocation texture) {
        return switch (pass.transparency()) {
            case SOLID -> RenderType.entitySolid(ResourceLocationBridge.toMc(texture));
            case ALPHA_TEST -> pass.disableCulling()
                    ? RenderType.entityCutoutNoCull(ResourceLocationBridge.toMc(texture))
                    : RenderType.entityCutout(ResourceLocationBridge.toMc(texture));
            case TRANSLUCENT -> pass.disableCulling()
                    ? RenderType.entityTranslucent(ResourceLocationBridge.toMc(texture))
                    : RenderType.entityTranslucentCull(ResourceLocationBridge.toMc(texture));
            case ADDITIVE -> RenderType.entityTranslucent(ResourceLocationBridge.toMc(texture));
        };
    }
}
