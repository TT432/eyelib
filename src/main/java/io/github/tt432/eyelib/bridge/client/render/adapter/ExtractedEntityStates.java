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
 * 26.1 render-state 路径的「渲染状态 → 实体」映射。
 * <p>
 * 26.1 的 {@code RenderLivingEvent} 不再携带实体。原实现只能按类型+坐标在
 * {@code level.entitiesForRendering()} 里反查，有两个失效场景：
 * <ul>
 *   <li>CarryOn 等模组用 {@code EntityType.create(NBT)} 造一个**不加入世界**的假实体，
 *       坐标反查永远找不到它（抱起萌化生物后一度退回 vanilla 外观）；</li>
 *   <li>两只同类型生物同格时，反查会把多份 render state 解析到同一个实体，导致该实体的模型与
 *       盔甲被画到每一份 state 的位置上（「多出一份本体/甲」）。</li>
 * </ul>
 * {@code EntityRenderDispatcher.extractEntity} 每次调用 {@code createRenderState} 都返回新对象，
 * 所以 state 身份可以安全作为键。
 * <p>
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
