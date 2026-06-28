package io.github.tt432.eyelib.client.render.pipeline;

/**
 * 帧管道的一个阶段，对 FramePlan 做原地变换。
 * <p>
 * 各阶段按 {@link FramePipeline} 持有的列表顺序执行，时序编码在列表顺序中。
 *
 * @author TT432
 */
@FunctionalInterface
public interface FrameStage {
    void apply(FramePlan plan);
}
