package io.github.tt432.eyelib.bridge.client.render.adapter;

import io.github.tt432.eyelib.bridge.ApplicationLifecyclePort;
import net.minecraft.world.entity.LivingEntity;
//? if <1.20.6 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
//?}

/** Clears derived attachable render state when a client entity leaves its level. */
//? if <1.20.6 {
@Mod.EventBusSubscriber(value = Dist.CLIENT)
//?} else {
@EventBusSubscriber(modid = "eyelib", value = Dist.CLIENT)
//?}
public final class EntityLeaveLevelEventAdapter {
    private EntityLeaveLevelEventAdapter() {
    }

    @SubscribeEvent
    public static void onEvent(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity livingEntity) {
            ApplicationLifecyclePort port = ApplicationLifecyclePort.get();
            if (port != null) port.onLivingEntityLeaveLevel(livingEntity);
        }
    }
}
