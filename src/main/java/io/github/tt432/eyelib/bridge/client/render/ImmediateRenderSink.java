package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * {@link RenderSink} 的立即实现（1.20.1 / 1.21.1）。
 *
 * <p>{@code submit} 同步从 {@link MultiBufferSource} 取 VertexConsumer 并执行 writer；
 * {@code flush} 在底层是 {@link MultiBufferSource.BufferSource} 时执行 {@code endBatch()} 触发立即绘制。
 * 行为与改造前的 getBuffer + 写 + flushBuffer 一致。
 *
 * @author TT432
 */
final class ImmediateRenderSink implements RenderSink {
    private final MultiBufferSource bufferSource;

    ImmediateRenderSink(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public void submit(PortRenderPass renderPass, PortResourceLocation texture,
                       PoseStack pose, GeometryWriter writer) {
        RenderType renderType = MaterialPort.toRenderType(renderPass, texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        writer.write(pose.last(), consumer);
    }

    @Override
    public void flush() {
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
    }
}
//?}
