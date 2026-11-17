package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.bridge.client.render.texture.NativeImagePort;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * 单个 {@link ModelComponent} 最终渲染输出的解析（renderPass + texture + isSolid），含 colorMask 替换。
 * <p>
 * 为什么单独成类：这段解析是「真实提交」与「主动预热」（{@link #resolve} 的两个消费方）共用的
 * 唯一键来源。复制一份到预热路径，就等于把 {@code clamped}/{@code _color_mask}/missing 纹理的派生规则
 * 复制出去（历史缺陷：下游 {@code EffectBakePreheat} 复制了 geometry→texture 配对，预热与真实键漂移）。
 * <p>
 * 不涉及 VertexConsumer——consumer 由 {@code RenderSink} 在回调中提供。
 *
 * @author TT432
 */
final class RenderOutputs {

    private RenderOutputs() {
    }

    /**
     * @param entity 宿主实体（仅用于 colorMask 的 tint 求值）；无实体宿主传 {@code null}
     * @return {@code null} 表示该组件没有有效渲染 pass
     */
    static @Nullable RenderOutput resolve(@Nullable Entity entity, ModelComponent modelComponent) {
        PortResourceLocation texture = modelComponent.getTexture();
        if (texture == null) {
            return null;
        }
        PortRenderPass renderPass = modelComponent.getRenderType(texture);
        boolean isSolid = modelComponent.isSolid();

        if (modelComponent.usesColorMask()) {
            float[] color = RenderPorts.get().renderSystemPort().getEntityTintColor(entity);
            if (color != null) {
                PortResourceLocation colorMaskTexture = NativeImagePort.colorMaskTexture(texture, color);
                if (colorMaskTexture != null) {
                    PortRenderPass colorMaskPass = modelComponent.getRenderType(colorMaskTexture);
                    if (colorMaskPass != null) {
                        texture = colorMaskTexture;
                        renderPass = colorMaskPass;
                    }
                }
            }
        }

        if (renderPass == null) {
            return null;
        }
        return new RenderOutput(renderPass, texture, isSolid);
    }

    record RenderOutput(PortRenderPass renderPass, PortResourceLocation texture, boolean isSolid) {
    }
}
