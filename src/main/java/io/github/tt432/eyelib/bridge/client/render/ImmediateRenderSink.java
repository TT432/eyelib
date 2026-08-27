package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningManager;
import io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningSession;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link RenderSink} 的立即实现（1.20.1 / 1.21.1）。
 *
 * <p>{@code submit} 同步从 {@link MultiBufferSource} 取 VertexConsumer 并执行 writer；
 * {@code flush} 在底层是 {@link MultiBufferSource.BufferSource} 时执行 {@code endBatch()} 触发立即绘制。
 * 行为与改造前的 getBuffer + 写 + endBatch 一致。
 *
 * @author TT432
 */
final class ImmediateRenderSink implements RenderSink {
    private final MultiBufferSource bufferSource;
    /** 本实体已完成的蒙皮会话（flush 时绘制，见 DESIGN-P2 §2）。 */
    private final List<LegacySkinningSession> skinnedDraws = new ArrayList<>();

    ImmediateRenderSink(MultiBufferSource bufferSource) {
        this.bufferSource = bufferSource;
    }

    @Override
    public void submit(PortRenderPass renderPass, PortResourceLocation texture,
                       PoseStack pose, GeometryWriter writer) {
        RenderType renderType = MaterialPort.toRenderType(renderPass, texture);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        // C1 GPU 蒙皮（P2）：会话非空时 writer 可跳过顶点写入（palette 采集），
        // 绘制由 flush 接管；routing RenderType 仅承担状态机与缓冲归类，共享缓冲保持为空。
        LegacySkinningSession skinning = LegacySkinningManager.createSession(renderType);
        try {
            writer.write(pose.last(), consumer, skinning);
        } catch (RuntimeException e) {
            if (skinning != null) {
                skinning.discard();
            }
            throw e;
        }
        if (skinning != null) {
            skinning.finish();
            if (skinning.hasDraw()) {
                skinnedDraws.add(skinning);
            }
        }
    }

    @Override
    public void flush() {
        if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
            bs.endBatch();
        }
        // 蒙皮绘制在 endBatch 之后：透明组件经 passOrder 排序后最后 flush，
        // 其蒙皮部分自然排在全部缓冲几何之后（与 26.1.2 半透明后画语义一致）
        for (LegacySkinningSession session : skinnedDraws) {
            LegacySkinningManager.draw(session);
        }
        skinnedDraws.clear();
    }

    @Override
    public MultiBufferSource multiBufferSource() {
        return bufferSource;
    }
}
//?}
