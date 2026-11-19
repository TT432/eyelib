//? if modern {
package io.github.tt432.eyelib.mixin.client;

import io.github.tt432.eyelib.bridge.client.render.adapter.ExtractedEntityStates;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.1：记录「活体 render state → 实体」映射，供渲染事件按**身份**解析实体。
 * <p>
 * 为什么不能只靠坐标反查：{@code RenderLivingEvent.Pre} 不携带实体，原先按「类型 + 插值坐标（±0.5 格）」
 * 在 {@code level.entitiesForRendering()} 里找。一旦两只同类型生物站在同一格（堆叠、同点召唤、CarryOn
 * 抱起后又在原地刷出），多个 render state 会解析到**同一个实体**：该实体的模型（及其盔甲/手持物）被
 * 重复画在每一份 state 的位置上，屏幕表现为「多出来的一份本体/甲」。另外 CarryOn 这类模组从 NBT 造的
 * 假实体根本不在 level 里，坐标反查永远找不到。
 * <p>
 * {@code extractEntity} 每次调用 {@code createRenderState} 都返回新对象，因此 state 身份可以安全作键。
 *
 * @author TT432
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void eyelib$recordEntityForRenderState(E entity, float partialTicks,
                                                                      CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (state instanceof LivingEntityRenderState && entity instanceof LivingEntity living) {
            ExtractedEntityStates.put(state, living);
        }
    }
}
//?}
