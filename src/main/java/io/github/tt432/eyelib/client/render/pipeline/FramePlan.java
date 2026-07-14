package io.github.tt432.eyelib.client.render.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 每帧的渲染计划，在各 {@link FrameStage} 间传递。
 * <p>
 * 每帧创建一个新实例，渲染线程单线程使用。各阶段通过 {@code xxxResults()} 列表写入结果。
 *
 * @author TT432
 */
public final class FramePlan {
    private final float partialTick;
    private final double camX;
    private final double camY;
    private final double camZ;
    private final long frameIndex;

    private final List<EntitySetupResult> setupResults = new ArrayList<>();
    private final List<Runnable> deferredEffects = new ArrayList<>();
    private final List<EntityTickResult> tickResults = new ArrayList<>();

    public FramePlan(float partialTick, double camX, double camY, double camZ, long frameIndex) {
        this.partialTick = partialTick;
        this.camX = camX;
        this.camY = camY;
        this.camZ = camZ;
        this.frameIndex = frameIndex;
    }

    public float partialTick() {
        return partialTick;
    }

    public double camX() {
        return camX;
    }

    public double camY() {
        return camY;
    }

    public double camZ() {
        return camZ;
    }

    public long frameIndex() {
        return frameIndex;
    }

    public List<EntitySetupResult> setupResults() {
        return setupResults;
    }

    public List<Runnable> deferredEffects() {
        return deferredEffects;
    }

    public List<EntityTickResult> tickResults() {
        return tickResults;
    }
}
