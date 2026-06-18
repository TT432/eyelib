package io.github.tt432.eyelib.mixin;

import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** @author TT432 */
@Mixin(RangedAttackGoal.class)
public interface RangedAttackGoalAccessor {
    @Accessor("attackTime")
    int eyelib$getAttackTime();

    @Accessor("attackTime")
    void eyelib$setAttackTime(int value);
}
