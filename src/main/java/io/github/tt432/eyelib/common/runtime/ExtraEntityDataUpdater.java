package io.github.tt432.eyelib.common.runtime;

import io.github.tt432.eyelib.capability.ExtraEntityData;

public final class ExtraEntityDataUpdater {
    private ExtraEntityDataUpdater() {
    }

    public static ExtraEntityData update(ExtraEntityData current, ObservedGoalFlags flags) {
        boolean facingTargetToRangeAttack = flags.facingTargetToRangeAttack();
        boolean isAvoidingMobs = flags.isAvoidingMobs();
        boolean isGrazing = flags.isGrazing();
        boolean isAvoid = flags.isAvoid();

        if (current.facing_target_to_range_attack() == facingTargetToRangeAttack
                && current.is_avoiding_mobs() == isAvoidingMobs
                && current.is_grazing() == isGrazing
                && current.is_avoid() == isAvoid) {
            return current;
        }

        return new ExtraEntityData(
                facingTargetToRangeAttack,
                isAvoidingMobs,
                isGrazing,
                isAvoid,
                current.is_dig()
        );
    }

    public record ObservedGoalFlags(
            boolean facingTargetToRangeAttack,
            boolean isAvoidingMobs,
            boolean isGrazing,
            boolean isAvoid
    ) {
    }
}
