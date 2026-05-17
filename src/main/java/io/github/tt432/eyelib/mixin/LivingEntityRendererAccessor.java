package io.github.tt432.eyelib.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author TT432
 */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    @Invoker
    float callGetWhiteOverlayProgress(LivingEntity le, float partialTicks);
}
