package io.github.tt432.eyelib.client.render.pipeline;

import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import net.minecraft.world.entity.Entity;

/**
 * 单个实体的 tick 阶段结果，记录动画输出数据。
 *
 * @author TT432
 */
public record EntityTickResult(Entity entity, ModelRuntimeData tickedInfos, AnimationEffects effects) {
}
