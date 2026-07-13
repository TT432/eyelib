package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if >=26.1 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * {@link RenderSink} 的延迟实现（26.1.2）。
 *
 * <p>{@code submit} 调用 {@link SubmitNodeCollector#submitCustomGeometry} 记录节点，
 * writer 回调在 {@code renderAllFeatures} 阶段（正确的 RenderPass 内）由 CustomFeatureRenderer 触发，
 * 把 eyelib 几何写入其提供的 VertexConsumer。{@code flush} 为空操作——绘制由帧渲染流程统一处理。
 *
 * <p>这是 26.1.2 延迟渲染架构下的正确路径，等价于 vanilla {@code GuiEntityRenderer} 的 submit 模式。
 *
 * @author TT432
 */
final class DeferredRenderSink implements RenderSink {
    private final SubmitNodeCollector collector;

    DeferredRenderSink(SubmitNodeCollector collector) {
        this.collector = collector;
    }

    @Override
    public void submit(PortRenderPass renderPass, PortResourceLocation texture,
                       PoseStack pose, GeometryWriter writer) {
        RenderType renderType = MaterialPort.toRenderType(renderPass, texture);
        collector.submitCustomGeometry(pose, renderType, (p, consumer) -> writer.write(p, consumer));
    }

    @Override
    public void flush() {
        // 无操作：renderAllFeatures 统一绘制已提交的 custom geometry。
    }
}
//?}
