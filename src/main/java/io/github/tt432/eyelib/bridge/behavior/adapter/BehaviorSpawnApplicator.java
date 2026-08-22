package io.github.tt432.eyelib.bridge.behavior.adapter;

import io.github.tt432.eyelib.behavior.BehaviorEntity;
import io.github.tt432.eyelib.behavior.BehaviorEntityRegistry;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.behavior.component.MarkVariant;
import io.github.tt432.eyelib.behavior.component.Variant;
import io.github.tt432.eyelib.behavior.component.group.ComponentGroup;
import io.github.tt432.eyelib.behavior.component.property.Scale;
import io.github.tt432.eyelib.behavior.event.logic.LogicNode;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.attachment.network.adapter.DataAttachmentSyncRuntime;
import io.github.tt432.eyelib.bridge.capability.EyelibAttachableData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;

import java.util.ArrayList;
import java.util.Optional;

/**
 * 行为包 spawn 逻辑应用器：求值 {@code minecraft:entity_spawned} 并把结果写入实体附件。
 * <p>
 * BE 语义：variant 由行为包 spawn 事件赋予，是实体变体的唯一真源；
 * JE 实体状态（如史莱姆尺寸层级）跟随 variant，不允许反向推导。
 *
 * @author TT432
 */
public final class BehaviorSpawnApplicator {
    private BehaviorSpawnApplicator() {
    }

    /**
     * 对新鲜生成的实体应用行为包 spawn 逻辑。
     *
     * @param syncToClient 是否向追踪者同步 {@link SyncedBehaviorState}（服务端调用为 true；
     *                     客户端 detached/dev 场景为 false）
     */
    public static void applyFreshSpawn(LivingEntity living, boolean syncToClient) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (key == null) return;

        BehaviorEntity be = BehaviorEntityRegistry.get(key.toString());
        if (be == null) return;

        LogicNode spawnEvent = be.events().get("minecraft:entity_spawned");
        ArrayList<ComponentGroup> groups = spawnEvent == null
                ? new ArrayList<>(be.component_groups().values())
                : new ArrayList<>();
        EntityBehaviorData data = new EntityBehaviorData(Optional.of(be), groups);
        if (spawnEvent != null) {
            // Add/Remove 节点自身维护组件索引（eval 内 setup），此处无需重复重建
            spawnEvent.eval(data);
        }

        Variant variant = data.component(Variant.class);
        Scale scale = data.component(Scale.class);
        MarkVariant markVariant = data.component(MarkVariant.class);

        SyncedBehaviorState state = new SyncedBehaviorState(
                variant != null ? variant.value() : 0,
                scale != null ? scale.value() : 1.0f,
                markVariant != null ? markVariant.value() : 0
        );

        applyVariantToEntity(living, state.variant());

        DataAttachmentHelper.setLocal(
                EyelibAttachableData.ENTITY_BEHAVIOR_DATA.get(),
                living,
                data
        );
        DataAttachmentHelper.setLocal(
                EyelibAttachableData.SYNCED_BEHAVIOR_STATE.get(),
                living,
                state
        );
        if (syncToClient) {
            DataAttachmentSyncRuntime.syncTrackedAndSelf(EyelibAttachableData.SYNCED_BEHAVIOR_STATE.get(), living, state);
        }
    }

    /**
     * 实体是否已持有同步行为状态。
     * <p>
     * BE 语义中 {@code minecraft:entity_spawned} 仅在生成时触发一次；已持有持久化状态的
     * 实体（如区块重载、世界重进）不得重新随机化 variant。
     */
    public static boolean hasSyncedState(LivingEntity living) {
        return DataAttachmentHelper.getOrNull(EyelibAttachableData.SYNCED_BEHAVIOR_STATE.get(), living) != null;
    }

    /**
     * 磁盘加载路径：不重新随机化，用已持久化的 variant 重新对齐 JE 实体状态。
     * variant 为真源——即使持久化状态由未联动尺寸的旧版本写入，加载后也会收敛为一致。
     */
    public static void reapplyVariantToEntity(LivingEntity living) {
        SyncedBehaviorState state = DataAttachmentHelper.getOrNull(
                EyelibAttachableData.SYNCED_BEHAVIOR_STATE.get(), living);
        if (state != null) {
            applyVariantToEntity(living, state.variant());
        }
    }

    /**
     * JE 实体状态跟随 BE variant：史莱姆/岩浆怪（JE 均为 {@link Slime} 子类）尺寸层级
     * 与 variant 同值（1/2/4，JE/BE 一致），随 variant 一并更新 hitbox、血量与经验。
     */
    private static void applyVariantToEntity(LivingEntity living, int variant) {
        if (variant <= 0) return;
        if (living instanceof Slime slime) {
            slime.setSize(variant, true);
        }
    }
}
