package io.github.tt432.eyelib.common.runtime;

import io.github.tt432.eyelib.capability.EntityStatistics;
import io.github.tt432.eyelib.capability.ExtraEntityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CommonRuntimeUpdaterTest {
    @Test
    void extraEntityDataUpdaterReturnsSameInstanceWhenFlagsDoNotChange() {
        ExtraEntityData current = new ExtraEntityData(true, false, true, false, true);

        ExtraEntityData updated = ExtraEntityDataUpdater.update(
                current,
                new ExtraEntityDataUpdater.ObservedGoalFlags(true, false, true, false)
        );

        assertSame(current, updated);
    }

    @Test
    void extraEntityDataUpdaterReplacesOnlyObservedGoalFlags() {
        ExtraEntityData current = new ExtraEntityData(false, false, false, false, true);

        ExtraEntityData updated = ExtraEntityDataUpdater.update(
                current,
                new ExtraEntityDataUpdater.ObservedGoalFlags(true, true, false, true)
        );

        assertEquals(true, updated.facing_target_to_range_attack());
        assertEquals(true, updated.is_avoiding_mobs());
        assertEquals(false, updated.is_grazing());
        assertEquals(true, updated.is_avoid());
        assertEquals(true, updated.is_dig());
    }

    @Test
    void entityStatisticsUpdaterAddsHorizontalDistanceOnly() {
        EntityStatistics current = new EntityStatistics(2.5f);

        EntityStatistics updated = EntityStatisticsUpdater.updateDistanceWalked(current, 3.0, 4.0);

        assertEquals(7.5f, updated.distanceWalked());
    }
}
