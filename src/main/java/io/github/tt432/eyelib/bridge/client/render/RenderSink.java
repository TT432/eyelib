package io.github.tt432.eyelib.bridge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.material.port.PassTier;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.bridge.client.render.skinning.SkinningSession;
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
 * <p>合批：调用方用 {@link #openBatch(BatchPolicy)} 包住一段提交（通常在整帧实体几何外围），
 * 作用域内的 {@link #flush()} 不再真正绘制，几何合并到作用域 close 时统一刷出。
 * 批次是 <b>sink 实例状态</b>（深度计数 + 策略），嵌套时只有最外层策略生效。
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
     * 刷出已提交的几何。立即实现执行 {@code endBatch()}（并绘制待画的蒙皮会话）；
     * 延迟实现无操作——26.1 的绘制发生在 {@code renderAllFeatures} 阶段，由 vanilla 按
     * RenderType 分组统一提交。
     *
     * <p>批次作用域内（{@link #openBatch}）本方法<b>不绘制</b>：绘制统一推迟到作用域最外层
     * {@link BatchScope#close()}。作用域外的行为与不开批次时完全一致。
     */
    void flush();

    /**
     * 打开一个批次作用域，使用保守策略 {@link BatchPolicy#DEFER}（只推迟 flush，不分桶）。
     *
     * @return 批次作用域，必须 try-with-resources；不支持的实现返回 {@link BatchScope#NOOP}
     */
    default BatchScope openBatch() {
        return openBatch(BatchPolicy.DEFER);
    }

    /**
     * 打开一个批次作用域。默认实现不支持批次：立即 {@link #flush()} 并返回
     * {@link BatchScope#NOOP}（既有实现不改也能编译且行为不变）。
     *
     * <p>实现契约（各 sink 的 javadoc 必须与之一致）：
     * <ul>
     *   <li>批次是 <b>sink 实例状态</b>：深度计数 + 策略；只有最外层 {@code openBatch} 的策略生效，
     *       嵌套 {@code openBatch} 的策略被忽略并 WARN 一次。</li>
     *   <li>作用域内 {@link #flush()} 不绘制；作用域外行为与今天完全一致。</li>
     *   <li>{@code mergeDraws=true} 时 sink 自建按 RenderType 分桶的几何缓冲，
     *       close 时按 {@link PassTier} 分组、组内按首次提交顺序各 draw 一次。</li>
     *   <li>关闭顺序固定：{@code OPAQUE} 桶 → OPAQUE 的蒙皮会话 → {@code BLEND} 桶 →
     *       BLEND 的蒙皮会话 → <b>最后</b> vanilla 通道 {@code endBatch()}
     *       （vanilla 通道装的是挂载物/手持物/栓绳的直写几何）。</li>
     *   <li>{@code allowGpuSkinning=false} 时向 writer 传 {@code skinning=null}。</li>
     *   <li>堆外资源：{@code close()} 与批次外的 {@link #flush()} 都释放桶；调用方忘记 close 时
     *       下一次 flush 兜底释放（几何丢失并 WARN 一次）。</li>
     * </ul>
     *
     * @param policy 批次策略，仅最外层生效
     * @return 批次作用域，必须 try-with-resources
     */
    default BatchScope openBatch(BatchPolicy policy) {
        flush();
        return BatchScope.NOOP;
    }

    /**
     * 本 sink 的绘制粒度；默认 {@link FlushMode#PER_SUBMIT}（保守：不假设可以跨 submit 合并）。
     *
     * @return 绘制粒度
     */
    default FlushMode flushMode() {
        return FlushMode.PER_SUBMIT;
    }

    /**
     * 查询该 pass + 纹理本次提交能否走 GPU 蒙皮，不可用时给出原因。
     *
     * <p>{@link SkinningSupport#AVAILABLE} 只是必要条件：模型维度仍可能拒绝蒙皮，
     * writer 必须保留 CPU 顶点写入回退。默认实现返回 {@link SkinningSupport#NO_ENGINE_PATH}
     * （无蒙皮引擎的 sink）。
     *
     * @param pass    端口渲染 pass
     * @param texture 纹理
     * @return 蒙皮可用性
     */
    default SkinningSupport skinningSupport(PortRenderPass pass, PortResourceLocation texture) {
        return SkinningSupport.NO_ENGINE_PATH;
    }

    /**
     * 已接收的 {@link #submit} 次数（单调累计，实例生命周期）。
     *
     * @return 提交次数；默认 0（不计数）
     */
    default int submitCount() {
        return 0;
    }

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
     *
     * @param skinning 非空表示本次提交可走 GPU 蒙皮：writer 可经会话采集骨骼调色板并跳过顶点写入；
     *                 会话 begin 返回 false 时必须回退经典 CPU 写入。会话来源：{@code <26.1}
     *                 由 {@code LegacySkinningManager.createSession}（VS / compute 两条路径）产生，
     *                 {@code >=26.1} 由 {@code NgSkinningManager.createSession} 产生；
     *                 批次策略 {@link BatchPolicy#allowGpuSkinning()} 为 false 时恒为 null。
     */
    @FunctionalInterface
    interface GeometryWriter {
        void write(PoseStack.Pose pose, VertexConsumer consumer, @org.jspecify.annotations.Nullable SkinningSession skinning);
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
