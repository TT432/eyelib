//? if legacy {
package io.github.tt432.eyelib.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.render.adapter.LeashSuppression;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.20.1：抑制被 eyelib 接管实体的原版栓绳。
 *
 * <p>该版本 {@code MobRenderer.render} 在 {@code super.render(...)} 返回后继续绘制原版栓绳
 * （不同于 1.21.1+ 把 leash 放在 {@code LivingEntityRenderer} 调用的 {@code EntityRenderer.render} 内，
 * 事件早退会一并跳过）。eyelib 已在 {@code renderComponents} 里用 Bedrock {@code lead} locator
 * 画绳，这里只在「本帧 eyelib 实际接管」的标记存在时取消原版后续段，未接管（doki=false /
 * 资源包缺失）时保持原版行为。
 *
 * @author TT432
 */
@Mixin(MobRenderer.class)
@SuppressWarnings({"unused", "PMD.UnusedPrivateMethod", "PMD.UnusedFormalParameter"})
public abstract class MobRendererLeashMixin {
    @Inject(
            method = "render(Lnet/minecraft/world/entity/Mob;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void eyelib$suppressVanillaLeash(Mob entity, float yaw, float partialTick,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, CallbackInfo ci) {
        if (LeashSuppression.consumeHandled(entity)) {
            ci.cancel();
        }
    }
}
//?}
