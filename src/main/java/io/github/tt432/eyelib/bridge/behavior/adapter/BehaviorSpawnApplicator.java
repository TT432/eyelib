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
import io.github.tt432.eyelib.bridge.capability.adapter.EyelibAttachableData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;

import java.util.ArrayList;
import java.util.Optional;

/**
 * 行为包 spawn 逻辑应用器：求值 {@code minecraft:entity_spawned} 并把结果写入实体附件。
 * <p>
 * BE 语义：variant 由行为包 spawn 事件赋予，是实体变体的真源。
 * <p>
 * 例外：史莱姆/岩浆怪的尺寸层级归 JE 生成逻辑所有——自然生成由 {@code Slime#finalizeSpawn}
 * 随机，分裂子代由 {@code Slime#remove} 从父尺寸派生（{@code size / 2}）。分裂子代进入世界时
 * 也走 {@code EntityJoinLevelEvent}，若此时按行为包随机 variant 回写尺寸，分裂结果会被重新
 * 随机化（issue #36）。因此这类实体的 variant 镜像 JE 尺寸，绝不由 variant 反向设置尺寸。
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

        // 尺寸维度归 JE：分裂子代已带父尺寸/2，禁止用随机 variant 回写（issue #36）。
        // spawn 事件仍决定其余组件（血量、碰撞盒等），只是尺寸层级以 JE 为准。
        int variantValue = living instanceof Slime slime
                ? slime.getSize()
                : variant != null ? variant.value() : 0;

        SyncedBehaviorState state = new SyncedBehaviorState(
                variantValue,
                scale != null ? scale.value() : 1.0f,
                markVariant != null ? markVariant.value() : 0
        );

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
}
