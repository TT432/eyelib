package io.github.tt432.eyelib.bridge.behavior;

import io.github.tt432.eyelib.bridge.behavior.adapter.BehaviorSpawnApplicator;
import net.minecraft.world.entity.LivingEntity;

/**
 * 行为包 spawn 逻辑 Port —— 隔离 application 对 adapter 具体实现的直接依赖（ADR-0018 I-5）。
 * <p>
 * BE 语义：variant 由行为包 spawn 事件赋予，是实体变体的唯一真源；
 * JE 实体状态（如史莱姆尺寸层级）跟随 variant，不允许反向推导。
 *
 * @author TT432
 */
public interface BehaviorSpawnPort {

    /**
     * 对新鲜生成的实体应用行为包 spawn 逻辑（求值 {@code minecraft:entity_spawned} 并写入附件）。
     *
     * @param syncToClient 是否向追踪者同步行为状态（服务端调用为 true；客户端 detached/dev 场景为 false）
     */
    static void applyFreshSpawn(LivingEntity living, boolean syncToClient) {
        BehaviorSpawnApplicator.applyFreshSpawn(living, syncToClient);
    }

    /**
     * 实体是否已持有同步行为状态。
     * <p>
     * BE 语义中 {@code minecraft:entity_spawned} 仅在生成时触发一次；已持有持久化状态的
     * 实体（如区块重载、世界重进）不得重新随机化 variant。
     */
    static boolean hasSyncedState(LivingEntity living) {
        return BehaviorSpawnApplicator.hasSyncedState(living);
    }

    /**
     * 磁盘加载路径：不重新随机化，用已持久化的 variant 重新对齐 JE 实体状态。
     */
    static void reapplyVariantToEntity(LivingEntity living) {
        BehaviorSpawnApplicator.reapplyVariantToEntity(living);
    }
}
