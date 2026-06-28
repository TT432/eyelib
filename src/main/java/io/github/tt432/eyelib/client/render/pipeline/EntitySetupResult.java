package io.github.tt432.eyelib.client.render.pipeline;

import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * 单个实体的 setup 阶段结果，含延迟副作用。
 *
 * @author TT432
 */
public record EntitySetupResult(Entity entity, List<Runnable> deferredEffects) {
}
