package io.github.tt432.eyelib.bridge.material;

import io.github.tt432.eyelib.bridge.material.adapter.RenderPassAdapter;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}

/**
 * 材质 Port：将 PortRenderPass 转换为 MC RenderType，避免 application 直接依赖 RenderPassAdapter。
 */
public interface MaterialPort {
    static RenderType toRenderType(PortRenderPass pass, PortResourceLocation texture) {
        return RenderPassAdapter.toRenderType(pass, texture);
    }
}
