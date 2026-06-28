package io.github.tt432.eyelib.client.render.pipeline;

import java.util.List;

/**
 * 帧管道，持有有序的 {@link FrameStage} 列表，按顺序对 {@link FramePlan} 执行各阶段。
 * <p>
 * 时序编码在列表顺序中，可遍历、可调试、可重放。
 *
 * @author TT432
 */
public final class FramePipeline {

    private final List<FrameStage> stages;

    public FramePipeline(List<FrameStage> stages) {
        this.stages = List.copyOf(stages);
    }

    public void run(FramePlan plan) {
        for (FrameStage stage : stages) {
            stage.apply(plan);
        }
    }
}
