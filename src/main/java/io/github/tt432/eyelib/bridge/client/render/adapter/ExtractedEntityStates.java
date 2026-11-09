//? if modern {
package io.github.tt432.eyelib.bridge.client.render.adapter;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 26.1 render-state 路径的「渲染状态 → 实体」映射，仅供**不在 level 中**的实体使用。
 * <p>
 * 26.1 的 {@code RenderLivingEvent} 不再携带实体，eyelib 只能按类型+坐标在
 * {@code level.entitiesForRendering()} 里反查。CarryOn 等模组会从 NBT 造一个**不加入世界**的
 * 假实体，用 {@code EntityRenderDispatcher.extractEntity} + {@code submit} 渲染——坐标反查永远
 * 找不到它，doki 模型因此退回原版模型（抱起萌化生物后变成 vanilla 外观）。
 * <p>
 * {@code EntityRenderDispatcher.extractEntity} 每次调用 {@code createRenderState} 都返回新对象，
 * 所以 state 身份可以安全作为键；只登记 level 反查不到的实体，正常世界实体不进入本表。
 * 弱键 + 弱值避免留住实体/渲染状态。
 *
 * @author TT432
 */
public interface ExtractedEntityStates {
    Map<EntityRenderState, WeakReference<LivingEntity>> STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    static void put(EntityRenderState state, LivingEntity entity) {
        STATES.put(state, new WeakReference<>(entity));
    }

    static @Nullable LivingEntity get(EntityRenderState state) {
        WeakReference<LivingEntity> reference = STATES.get(state);
        return reference == null ? null : reference.get();
    }
}
//?}
