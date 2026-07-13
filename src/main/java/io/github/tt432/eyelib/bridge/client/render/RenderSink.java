package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
//? if <26.1 {
import net.minecraft.client.renderer.MultiBufferSource;
//?} else {
import net.minecraft.client.renderer.SubmitNodeCollector;
//?}

/**
 * 渲染输出 Port：屏蔽「立即绘制」(1.20.1/1.21.1) 与「延迟提交」(26.1.2) 的差异。
 *
 * <p>业务层（{@code EntityRenderOrchestrator.renderComponents}）把每段几何的顶点生成逻辑
 * 打包为 {@link GeometryWriter} 回调，通过 {@link #submit} 交给本 Port。
 * <ul>
 *   <li>{@code <26.1}：立即实现从 {@code MultiBufferSource} 取 VertexConsumer，
 *       同步执行 writer，{@link #flush()} 时 {@code endBatch()} 绘制。</li>
 *   <li>{@code >=26.1}：延迟实现调用 {@code SubmitNodeCollector.submitCustomGeometry} 记录节点，
 *       writer 在 {@code renderAllFeatures} 阶段（正确 RenderPass 内）执行；{@link #flush()} 为空操作。</li>
 * </ul>
 * 三版本的业务写法完全一致，仅本 Port 的实现分版本。
 *
 * @author TT432
 */
public interface RenderSink {

    /**
     * 提交一段几何。renderPass + texture 决定 MC RenderType，writer 写顶点到 sink 提供的 VertexConsumer。
     *
     * @param renderPass 端口渲染 pass（解析为 MC RenderType）
     * @param texture    纹理（参与 RenderType 构造）
     * @param pose       当前 PoseStack（延迟实现会拷贝其快照）
     * @param writer     顶点生成回调
     */
    void submit(PortRenderPass renderPass, PortResourceLocation texture,
                PoseStack pose, GeometryWriter writer);

    /**
     * 刷出已提交的几何。立即实现执行 {@code endBatch()}；延迟实现无操作（由 renderAllFeatures 绘制）。
     */
    void flush();

    /**
     * 返回底层 vanilla 渲染目标，供附属渲染（如手持物品）直接调用 vanilla API。
     * <p>&lt;26.1 返回 {@link MultiBufferSource}，&gt;=26.1 返回 {@link SubmitNodeCollector}。
     */
    //? if <26.1 {
    MultiBufferSource multiBufferSource();
    //?} else {
    SubmitNodeCollector submitNodeCollector();
    //?}

    /**
     * 顶点生成回调：把几何写入给定 VertexConsumer。
     */
    @FunctionalInterface
    interface GeometryWriter {
        void write(PoseStack.Pose pose, VertexConsumer consumer);
    }

    //? if <26.1 {
    /**
     * 立即实现：包住一个 {@link MultiBufferSource}。
     */
    static RenderSink of(MultiBufferSource bufferSource) {
        return new ImmediateRenderSink(bufferSource);
    }
    //?} else {
    /**
     * 延迟实现：包住 {@link SubmitNodeCollector}。
     */
    static RenderSink of(SubmitNodeCollector collector) {
        return new DeferredRenderSink(collector);
    }
    //?}
}
