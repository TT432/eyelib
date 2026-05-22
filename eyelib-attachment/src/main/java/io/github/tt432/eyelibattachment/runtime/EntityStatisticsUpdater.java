package io.github.tt432.eyelibattachment.runtime;

import io.github.tt432.eyelibattachment.capability.EntityStatistics;

/**
 * @author TT432
 */
public final class EntityStatisticsUpdater {
    private EntityStatisticsUpdater() {
    }

    public static EntityStatistics updateDistanceWalked(EntityStatistics current, double deltaX, double deltaZ) {
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        return new EntityStatistics(current.distanceWalked() + distance);
    }
}