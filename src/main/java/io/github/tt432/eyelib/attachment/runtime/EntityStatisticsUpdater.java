package io.github.tt432.eyelib.attachment.runtime;

import io.github.tt432.eyelib.attachment.capability.EntityStatistics;

/**
 * 实体统计数据的更新工具。
 *
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