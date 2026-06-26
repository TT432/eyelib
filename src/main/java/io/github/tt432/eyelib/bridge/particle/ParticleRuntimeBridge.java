package io.github.tt432.eyelib.bridge.particle;

import io.github.tt432.eyelib.particle.ParticleRenderManager;
import io.github.tt432.eyelib.particle.ParticleSpawnRuntimeAdapter;
import io.github.tt432.eyelib.particle.loading.ParticleDefinitionRegistry;

import java.util.Optional;

/**
 * 持有 particle runtime 的客户端单例，注入 MC 线程提交实现。
 * 实例级 supplier 默认空，实际 supplier 由 {@code ParticleSpawnRuntimeAdapter.configure()} 注入。
 */
/** @author TT432 */
public final class ParticleRuntimeBridge {
    public static final ParticleRenderManager RENDER_MANAGER =
            new ParticleRenderManager(MinecraftParticleSubmitter.INSTANCE);

    public static final ParticleSpawnRuntimeAdapter SPAWN_ADAPTER =
            new ParticleSpawnRuntimeAdapter(
                    ParticleDefinitionRegistry.store(),
                    RENDER_MANAGER,
                    Optional::empty,
                    Optional::empty
            );

    private ParticleRuntimeBridge() {
    }
}

