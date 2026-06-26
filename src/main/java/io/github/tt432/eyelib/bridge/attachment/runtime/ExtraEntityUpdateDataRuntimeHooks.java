package io.github.tt432.eyelib.bridge.attachment.runtime;

import io.github.tt432.eyelib.util.entitydata.ExtraEntityUpdateData;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelib.bridge.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.bridge.attachment.network.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.bridge.network.EyelibNetworkTransport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
//? if <1.20.6 {
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
//?}
/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
//? if <1.20.6 {
@Mod.EventBusSubscriber(modid = "eyelib", bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} else {
@EventBusSubscriber(modid = "eyelib", bus = EventBusSubscriber.Bus.GAME)
//?}
public final class ExtraEntityUpdateDataRuntimeHooks {
    @SubscribeEvent
    //? if <1.20.6 {
    public static void onLivingDamage(LivingDamageEvent event) {
    //?} else {
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
    //?}
        Entity directEntity = event.getSource().getDirectEntity();

        if (directEntity != null) {
            Vec3 damageSourcePosition = directEntity.position();
            LivingEntity entity = event.getEntity();
            var key = DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get();
            ExtraEntityUpdateData data = DataAttachmentHelper.getOrCreate(key, entity);
            Vec3 entityPosition = entity.position();
            ExtraEntityUpdateData updated = updateFromEntity(data, entity)
                    .withLastHurtX(damageSourcePosition.x() - entityPosition.x())
                    .withLastHurtY(damageSourcePosition.y() - entityPosition.y())
                    .withLastHurtZ(damageSourcePosition.z() - entityPosition.z());

            if (data != updated) {
                DataAttachmentHelper.setLocal(key, entity, updated);
                EyelibNetworkTransport.sendToTrackedAndSelf(entity, new ExtraEntityUpdateDataPacket(entity.getId(), updated));
            }
        }
    }

    @SubscribeEvent
    //? if <1.20.6 {
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
    //?} elif <26.1 {
    public static void onLivingTick(EntityTickEvent event) {
    //?} else {
    public static void onLivingTick(EntityTickEvent.Pre event) {
    //?}
        Entity entity = event.getEntity();
        var key = DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get();
        ExtraEntityUpdateData data = DataAttachmentHelper.getOrCreate(key, entity);
        ExtraEntityUpdateData updated = updateFromEntity(data, entity);

        //? if <26.1 {
        if (!entity.level().isClientSide && data != updated) {
        //?} else {
        if (!entity.level().isClientSide() && data != updated) {
        //?}
            DataAttachmentHelper.setLocal(key, entity, updated);
            EyelibNetworkTransport.sendToTrackedAndSelf(entity, new ExtraEntityUpdateDataPacket(entity.getId(), updated));
        }
    }

    private static ExtraEntityUpdateData updateFromEntity(ExtraEntityUpdateData data, Entity entity) {
        var updated = data;

        if (entity instanceof Mob targeting) {
            //? if <26.1 {
            if (!entity.level().isClientSide) {
            //?} else {
            if (!entity.level().isClientSide()) {
            //?}
                if (targeting.getTarget() != null) {
                    updated = updated.withTargetId(targeting.getTarget().getId());
                } else {
                    updated = updated.withTargetId(-1);
                }
            } else if ((targeting.getTarget() == null || targeting.getTarget().getId() != data.targetId())
                    && data.targetId() != -1) {
                targeting.setTarget((LivingEntity) targeting.level().getEntity(data.targetId()));
            }
        }

        //? if <26.1 {
        if (entity instanceof Mob mob && !entity.level().isClientSide) {
        //?} else {
        if (entity instanceof Mob mob && !entity.level().isClientSide()) {
        //?}
            float speed;

            if (!mob.getNavigation().isDone()) {
                speed = mob.getSpeed();
            } else {
                speed = 0;
            }

            if (speed != data.speed()) {
                updated = updated.withSpeed(speed);
            }
        }

        return updated;
    }
}
