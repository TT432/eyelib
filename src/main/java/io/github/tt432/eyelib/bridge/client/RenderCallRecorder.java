package io.github.tt432.eyelib.bridge.client;

/**
 * 将任务记录到渲染线程执行，封装 blaze3d RenderSystem 版本差异。
 *
 * @author TT432
 */
public final class RenderCallRecorder {
    private RenderCallRecorder() {
    }

    public static void record(Runnable runnable) {
        //? if <26.1 {
        com.mojang.blaze3d.systems.RenderSystem.recordRenderCall(runnable::run);
        //?} else {
        throw new UnsupportedOperationException("26.1 migration");
        //?}
    }
}
