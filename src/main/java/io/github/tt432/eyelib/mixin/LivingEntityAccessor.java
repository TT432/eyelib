package io.github.tt432.eyelib.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** @author TT432 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("jumping")
    boolean eyelib$isJumping();

    @Invoker("updateSwingTime")
    void eyelib$invokeUpdateSwingTime();
}
