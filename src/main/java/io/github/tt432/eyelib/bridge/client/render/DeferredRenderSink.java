package io.github.tt432.eyelib.bridge.client.render;
//? if >=26.1 {
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.material.MaterialPort;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * {@link RenderSink} 的延迟实现（26.1.2）。
 *
 * <p>{@code submit} 经 {@link SubmitNodeCollector#submitCustomGeometry} 注册节点，
 * writer 回调在 {@code renderAllFeatures}（{@code renderSolid}/{@code renderTranslucent}）阶段触发，
 * 直接写入 CustomFeatureRenderer 按 RenderType 分组共享的 BufferBuilder——phase 末由 LevelRenderer
 * {@code endBatch} 统一绘制，同 RenderType 的多实体几何合并为一次 draw call。
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
        // 直接写 CustomFeatureRenderer 提供的共享 buffer：vanilla 按 RenderType 分组、
        // 每组共享一个 BufferBuilder，phase 末由 LevelRenderer endBatch 统一绘制——真正的批量路径。
        collector.submitCustomGeometry(pose, renderType, writer::write);
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
