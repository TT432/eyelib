package io.github.tt432.eyelib.bridge;

import io.github.tt432.eyelib.bridge.behavior.adapter.BehaviorSpawnApplicator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
//? if <1.20.6 {
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
//?}

/**
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = Eyelib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
@EventBusSubscriber(modid = Eyelib.MOD_ID)
//?}
public final class CommonEntityEventHandler {
    private CommonEntityEventHandler() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ApplicationLifecyclePort port = ApplicationLifecyclePort.get();
        if (port != null) port.loadBehaviorPacks(event.getServer());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof LivingEntity living)) return;

        // BE 语义：entity_spawned 仅在生成时触发一次；已持有持久化 SyncedBehaviorState 的
        // 实体（区块重载、世界重进）不得重新随机化 variant，但仍需用持久化 variant 重新对齐
        // JE 实体状态（史莱姆尺寸）——variant 为真源，旧版本写下的不一致状态随之收敛。
        if (BehaviorSpawnApplicator.hasSyncedState(living)) {
            BehaviorSpawnApplicator.reapplyVariantToEntity(living);
            return;
        }

        BehaviorSpawnApplicator.applyFreshSpawn(living, true);
    }
}

