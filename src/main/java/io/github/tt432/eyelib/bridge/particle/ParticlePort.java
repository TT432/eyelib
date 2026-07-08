package io.github.tt432.eyelib.bridge.particle;

import io.github.tt432.eyelib.bridge.particle.adapter.ParticleRuntimeBridge;
import io.github.tt432.eyelib.particle.ParticleSpawnRuntimeAdapter;

/**
 * ParticleRuntimeBridge Port —— 隔离 application 对 adapter 具体类的直接依赖。
 */
public interface ParticlePort {
    static ParticleSpawnRuntimeAdapter getSpawnAdapter() {
        return ParticleRuntimeBridge.SPAWN_ADAPTER;
    }
}
