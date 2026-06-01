package io.github.tt432.eyelib.capability;

import io.github.tt432.eyelibattachment.capability.ExtraEntityUpdateData;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelibattachment.network.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelibnetwork.EyelibNetworkTransport;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
@NullMarked
public final class ExtraEntityUpdateDataRuntimeHooks {
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
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
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        Entity entity = event.getEntity();
        var key = DataAttachmentTypeRegistry.EXTRA_ENTITY_UPDATE.get();
        ExtraEntityUpdateData data = DataAttachmentHelper.getOrCreate(key, entity);
        ExtraEntityUpdateData updated = updateFromEntity(data, entity);

        if (!entity.level().isClientSide && data != updated) {
            DataAttachmentHelper.setLocal(key, entity, updated);
            EyelibNetworkTransport.sendToTrackedAndSelf(entity, new ExtraEntityUpdateDataPacket(entity.getId(), updated));
        }
    }

    private static ExtraEntityUpdateData updateFromEntity(ExtraEntityUpdateData data, Entity entity) {
        var updated = data;

        if (entity instanceof Mob targeting) {
            if (!entity.level().isClientSide) {
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

        if (entity instanceof Mob mob && !entity.level().isClientSide) {
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
