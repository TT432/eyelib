package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.bridge.client.loader.ResourceLoader;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 资源包切换时清空盔甲渲染缓存（烘焙几何 + 装备素材解析结果）。
 * 通过 {@code @ResourceLoader} 自动注册，避免盔甲几何/纹理在 F3+T 后仍用旧资源。
 *
 * @author TT432
 */
@ResourceLoader
public final class ArmorRenderCacheLoader implements PreparableReloadListener {
    ArmorRenderCacheLoader() {
    }

    //? if <26.1 {
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        ArmorRenderAdapter.invalidate();
        return barrier.wait(Unit.INSTANCE).thenRun(() -> {
        });
    }
    //?} else {
    @Override
    public CompletableFuture<Void> reload(SharedState currentReload, Executor taskExecutor,
                                          PreparationBarrier barrier, Executor reloadExecutor) {
        ArmorRenderAdapter.invalidate();
        return barrier.wait(Unit.INSTANCE).thenRun(() -> {
        });
    }
    //?}
}
