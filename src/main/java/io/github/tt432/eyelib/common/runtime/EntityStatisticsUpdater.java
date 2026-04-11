package io.github.tt432.eyelib.common.runtime;

import io.github.tt432.eyelib.capability.EntityStatistics;

public final class EntityStatisticsUpdater {
    private EntityStatisticsUpdater() {
    }

    public static EntityStatistics updateDistanceWalked(EntityStatistics current, double deltaX, double deltaZ) {
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        return new EntityStatistics(current.distanceWalked() + distance);
    }
}
