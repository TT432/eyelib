package io.github.tt432.eyelib.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@Mixin(LivingEntityRenderer.class)
@NullMarked
public interface LivingEntityRendererAccessor {
    @Invoker
    float callGetWhiteOverlayProgress(LivingEntity le, float partialTicks);
}