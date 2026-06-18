package io.github.tt432.eyelib;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.common.behavior.BehaviorEntityRegistry;
import io.github.tt432.eyelib.common.behavior.BehaviorPackAutoLoader;
import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.attachment.network.DataAttachmentSyncRuntime;
import io.github.tt432.eyelib.behavior.BehaviorEntity;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.behavior.component.MarkVariant;
import io.github.tt432.eyelib.behavior.component.Variant;
import io.github.tt432.eyelib.behavior.component.group.ComponentGroup;
import io.github.tt432.eyelib.behavior.component.property.Scale;
import io.github.tt432.eyelib.behavior.event.logic.LogicNode;
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
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author TT432
 */
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = Eyelib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
@EventBusSubscriber(modid = Eyelib.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
//?}
public final class CommonEntityEventHandler {
    private CommonEntityEventHandler() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        BehaviorPackAutoLoader.load(event.getServer());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof LivingEntity living)) return;

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
            spawnEvent.eval(data);
            data.setup();
        }

        Variant variant = data.component(Variant.class);
        Scale scale = data.component(Scale.class);
        MarkVariant markVariant = data.component(MarkVariant.class);

        SyncedBehaviorState state = new SyncedBehaviorState(
                variant != null ? variant.value() : 0,
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
        DataAttachmentSyncRuntime.syncTrackedAndSelf(EyelibAttachableData.SYNCED_BEHAVIOR_STATE.get(), living, state);
    }
}
