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
 * 26.1：记录「不在世界中的活体实体」的 render state → 实体映射。
 * <p>
 * 正常世界实体由 {@code RenderLivingEventAdapter} 按类型+坐标反查，不需要登记；
 * 只有 CarryOn 这类从 NBT 生成的假实体（{@code level.getEntity(id) != entity}）才登记，
 * 让 doki 渲染在 off-level 实体上也能接管。见 {@link ExtractedEntityStates}。
 *
 * @author TT432
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "extractEntity", at = @At("RETURN"))
    private <E extends Entity> void eyelib$recordOffLevelEntity(E entity, float partialTicks,
                                                                CallbackInfoReturnable<EntityRenderState> cir) {
        EntityRenderState state = cir.getReturnValue();
        if (state instanceof LivingEntityRenderState && entity instanceof LivingEntity living) {
            if (entity.level().getEntity(entity.getId()) != entity) {
                ExtractedEntityStates.put(state, living);
            }
        }
    }
}
//?}
