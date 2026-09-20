package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.bridge.molang.adapter.EntityPortAdapter;
import io.github.tt432.eyelib.molang.port.PortEntity;
import net.minecraft.world.entity.Entity;

/**
 * 实体 Port：将 MC Entity 翻译为 PortEntity，避免 application 直接依赖 EntityPortAdapter。
 */
public interface EntityPort {
    static PortEntity from(Entity entity) {
        return EntityPortAdapter.from(entity);
    }
    /** 缓存版：同一实体返回同一 PortEntity 实例，配合 MolangScope 宿主幂等写短路（每帧 tick 路径用）。 */
    static PortEntity fromCached(Entity entity) {
        return EntityPortAdapter.fromCached(entity);
    }
}
