//? if >=1.20.6 {
package io.github.tt432.eyelib.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** @author TT432 */
@Mixin(AvoidEntityGoal.class)
public interface AvoidEntityGoalAccessor {
    @Accessor("toAvoid")
    LivingEntity eyelib$getToAvoid();
}
//?}
