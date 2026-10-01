//? if <1.20.6 {
package io.github.tt432.eyelib.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.bridge.client.render.adapter.TextureColorWorldPass;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在调用方注入，确保晚于 Oculus 的 LevelRenderer RETURN 合成回调。 */
@Mixin(GameRenderer.class)
public abstract class TextureColorGameRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void eyelib$beginTextureColor(float partialTick, long finishTime, PoseStack pose, CallbackInfo ci) {
        TextureColorWorldPass.begin();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V"))
    private void eyelib$drawTextureColor(float partialTick, long finishTime, PoseStack pose, CallbackInfo ci) {
        TextureColorWorldPass.draw();
    }
}
//?}
