package io.github.tt432.eyelib.common;

import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.network.ExtraEntityDataPacket;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;

/**
 * @author TT432
 */
@EventBusSubscriber
public class EntityVariantHandler {
    private static final ArrayList<Runnable> queue1 = new ArrayList<>();
    private static final ArrayList<Runnable> queue2 = new ArrayList<>();

    @SubscribeEvent
    public static void onEvent(ServerTickEvent.Pre event) {
        queue1.forEach(Runnable::run);
        queue1.clear();
        queue1.addAll(queue2);
        queue2.clear();
    }

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Zombie z && !z.level().isClientSide) {
            ExtraEntityData oldData = z.getData(EyelibAttachableData.EXTRA_ENTITY_DATA);
            if (oldData.variant() == -1) {
                ExtraEntityData data = oldData.withVariant(z.getRandom().nextInt(3));
                z.setData(EyelibAttachableData.EXTRA_ENTITY_DATA, data);
                queue2.add(() -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(z, new ExtraEntityDataPacket(z.getId(), data)));
            }
        }
    }
}
