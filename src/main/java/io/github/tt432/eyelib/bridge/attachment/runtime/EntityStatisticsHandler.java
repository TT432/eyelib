package io.github.tt432.eyelib.bridge.attachment.runtime;

import io.github.tt432.eyelib.util.entitydata.EntityStatisticsUpdater;
import io.github.tt432.eyelib.util.entitydata.EntityStatistics;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.adapter.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.bridge.attachment.network.adapter.DataAttachmentSyncRuntime;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
//? if <1.20.6 {
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
//?}

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 实体统计数据的事件处理器。
 * <p>
 * 记账只在逻辑服务端进行，客户端仅接收 {@link DataAttachmentSyncRuntime} 下发的绝对值。
 * 2026-10-30 实机确证：处理器在客户端与服务端都会收到 tick 事件（单机同一 JVM），
 * 而 {@code LAST_POS} 是静态表，两侧线程并发读写会丢更新，导致 last 落后数 tick、
 * 单次位移被放大 3~4 倍；同时客户端本地累加值会被服务端同步的绝对值覆盖，
 * 表现为 anim_time 抖动/回退（蜘蛛行走抽搐、移动动画平移）。
 *
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber
//?} else {
@EventBusSubscriber(modid = "eyelib")
//?}
public class EntityStatisticsHandler {
    /**
     * 自记上一 tick 位置：Forge 1.20.1 LivingTickEvent 与 NeoForge EntityTickEvent.Pre
     * 触发时 entity.xo 已等于当前位置（2026-10-12 实机探针：26175 次触发位移和恒 0），
     * 用 xo 差值统计会让 distanceWalked 永远为 0，modified_distance_moved 驱动的动画
     * （蜘蛛 walk 等）anim_time 冻结→模型平移。
     * <p>
     * 必须线程安全：单机下客户端与服务端共享同一份静态表。
     */
    private static final Map<Entity, Vec3> LAST_POS = Collections.synchronizedMap(new WeakHashMap<>());

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onEvent(LivingEvent.LivingTickEvent event) {
    //?} else {
    public static void onEvent(EntityTickEvent.Pre event) {
    //?}
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        Vec3 pos = entity.position();
        Vec3 last = LAST_POS.put(entity, pos);
        if (last == null) return;

        double x = pos.x - last.x;
        double z = pos.z - last.z;
        if (x == 0 && z == 0) return;

        EntityStatistics data = DataAttachmentHelper.getOrCreate(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), entity);
        EntityStatistics updated = EntityStatisticsUpdater.updateDistanceWalked(data, x, z);
        DataAttachmentHelper.setLocal(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), entity, updated);
        DataAttachmentSyncRuntime.syncTrackedAndSelf(DataAttachmentTypeRegistry.ENTITY_STATISTICS.get(), entity, updated);
    }
}
