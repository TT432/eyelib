package io.github.tt432.eyelib.bridge.client.render;
//? if >=26.1 {
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * {@link RenderSink} 的延迟实现（26.1.2）。
 *
 * <p>{@code submit} 经 {@link SubmitNodeCollector#submitCustomGeometry} 注册节点，
 * writer 回调在 {@code renderAllFeatures}（{@code renderSolid}/{@code renderTranslucent}）阶段触发。
 *
 * <p><b>为何不直接写 CustomFeatureRenderer 提供的 consumer：</b>FeatureRenderDispatcher 的共享
 * {@link MultiBufferSource.BufferSource} 会跨帧缓存已 {@code build()} 的 BufferBuilder，对 QUADS 模式
 * （{@code canConsolidateConsecutiveGeometry=true}）的 RenderType 返回 stale（{@code building=false}）
 * 的 builder，导致 {@code addVertex} 抛 "Not building!"。因此回调内用独立 immediate BufferSource 写入
 * 并立即 {@code endBatch} 绘制——renderSolid 阶段投影/光图/输出目标均已就绪，立即绘制合法。
 *
 * <p>{@code flush} 为空操作。
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
        collector.submitCustomGeometry(pose, renderType, (p, consumer) -> {
            // 独立 immediate BufferSource：避开共享 bufferSource 的 stale-builder 问题。
            try (ByteBufferBuilder bb = new ByteBufferBuilder(786432)) {
                MultiBufferSource.BufferSource bs = MultiBufferSource.immediate(bb);
                writer.write(p, bs.getBuffer(renderType));
                bs.endBatch();
            }
        });
    }

    @Override
    public void flush() {
        // 无操作：每个 submit 回调内已立即绘制。
    }

    @Override
    public SubmitNodeCollector submitNodeCollector() {
        return collector;
    }
}
//?}
