package io.github.tt432.eyelib.bridge.material;

import io.github.tt432.eyelib.material.port.PortRenderPass;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
/**
 * 携带已构造 MC RenderType 的桥接渲染 pass。
 *
 * @author TT432
 */
public record BridgeRenderPass(
        PortRenderPass.Transparency transparency,
        boolean disableCulling,
        RenderType renderType
) implements PortRenderPass {
}
