package io.github.tt432.eyelib.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
//? if >=26.1 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 读取原版渲染器的 {@code getWhiteOverlayProgress}（受伤/自爆白闪进度）：
 * eyelib 接管渲染后需要用它构造与原版一致的 overlay UV，否则 creeper 的白闪等
 * 依赖该进度的效果会丢失。
 *
 * @author TT432
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    //? if <26.1 {
    @Invoker
    float callGetWhiteOverlayProgress(LivingEntity le, float partialTicks);
    //?} else {
    @Invoker
    float callGetWhiteOverlayProgress(LivingEntityRenderState state);
    //?}
}
