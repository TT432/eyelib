package io.github.tt432.eyelib.capability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.network.ExtraEntityUpdateDataPacket;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import io.github.tt432.eyelib.util.codec.stream.EyelibStreamCodecs;
import io.github.tt432.eyelib.util.codec.stream.StreamCodec;
import io.github.tt432.eyelib.util.data_attach.DataAttachmentHelper;
import lombok.With;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @author TT432
 */
@With
public record ExtraEntityUpdateData(
        int targetId,
        double lastHurtX,
        double lastHurtY,
        double lastHurtZ,
        float speed
) {
    public static final Codec<ExtraEntityUpdateData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.INT.fieldOf("targetId").forGetter(ExtraEntityUpdateData::targetId),
            Codec.DOUBLE.fieldOf("lastHurtX").forGetter(ExtraEntityUpdateData::lastHurtX),
            Codec.DOUBLE.fieldOf("lastHurtY").forGetter(ExtraEntityUpdateData::lastHurtY),
            Codec.DOUBLE.fieldOf("lastHurtZ").forGetter(ExtraEntityUpdateData::lastHurtZ),
            Codec.FLOAT.fieldOf("speed").forGetter(ExtraEntityUpdateData::speed)
    ).apply(ins, ExtraEntityUpdateData::new));

    public static final StreamCodec<ExtraEntityUpdateData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(ExtraEntityUpdateData obj, FriendlyByteBuf buf) {
            EyelibStreamCodecs.VAR_INT.encode(obj.targetId, buf);
            EyelibStreamCodecs.DOUBLE.encode(obj.lastHurtX, buf);
            EyelibStreamCodecs.DOUBLE.encode(obj.lastHurtY, buf);
            EyelibStreamCodecs.DOUBLE.encode(obj.lastHurtZ, buf);
            EyelibStreamCodecs.FLOAT.encode(obj.speed, buf);
        }

        @Override
        public ExtraEntityUpdateData decode(FriendlyByteBuf buf) {
            var targetId = EyelibStreamCodecs.VAR_INT.decode(buf);
            var lastHurtX = EyelibStreamCodecs.DOUBLE.decode(buf);
            var lastHurtY = EyelibStreamCodecs.DOUBLE.decode(buf);
            var lastHurtZ = EyelibStreamCodecs.DOUBLE.decode(buf);
            var speed = EyelibStreamCodecs.FLOAT.decode(buf);
            return new ExtraEntityUpdateData(targetId, lastHurtX, lastHurtY, lastHurtZ, speed);
        }
    };

    public static ExtraEntityUpdateData empty() {
        return new ExtraEntityUpdateData(-1, 0, 0, 0, 0);
    }

    public ExtraEntityUpdateData update(Entity entity) {
        var r = this;

        if (entity instanceof Mob targeting) {
            if (!entity.level().isClientSide) {
                if (targeting.getTarget() != null) {
                    r = r.withTargetId(targeting.getTarget().getId());
                } else {
                    r = r.withTargetId(-1);
                }
            } else {
                if ((targeting.getTarget() == null || targeting.getTarget().getId() != targetId) && targetId != -1) {
                    targeting.setTarget((LivingEntity) targeting.level().getEntity(targetId));
                }
            }
        }

        if (entity instanceof Mob l) {
            if (!entity.level().isClientSide) {
                float speed;

                if (!l.getNavigation().isDone()) {
                    speed = l.getSpeed();
                } else {
                    speed = 0;
                }

                if (speed != this.speed) {
                    r = r.withSpeed(speed);
                }
            }
        }

        return r;
    }

    @Mod.EventBusSubscriber
    public static final class Events {
        @SubscribeEvent
        public static void onEvent(LivingDamageEvent event) {
            Entity directEntity = event.getSource().getDirectEntity();
            var key = EyelibAttachableData.EXTRA_ENTITY_UPDATE.get();

            if (directEntity != null) {
                Vec3 depos = directEntity.position();
                LivingEntity entity = event.getEntity();
                ExtraEntityUpdateData data = DataAttachmentHelper.getOrCreate(key, entity);
                Vec3 epos = entity.position();
                ExtraEntityUpdateData updated = data.update(entity)
                        .withLastHurtX(depos.x() - epos.x())
                        .withLastHurtY(depos.y() - epos.y())
                        .withLastHurtZ(depos.z() - epos.z());

                if (data != updated) {
                    DataAttachmentHelper.set(key, entity, updated);
                    EyelibNetworkManager.sendToTrackedAndSelf(entity, new ExtraEntityUpdateDataPacket(entity.getId(), updated));
                }
            }
        }

        @SubscribeEvent
        public static void onEvent(LivingEvent.LivingTickEvent event) {
            Entity entity = event.getEntity();
            var key = EyelibAttachableData.EXTRA_ENTITY_UPDATE.get();
            ExtraEntityUpdateData data = DataAttachmentHelper.getOrCreate(key, entity);
            ExtraEntityUpdateData updated = data.update(entity);

            if (!entity.level().isClientSide && data != updated) {
                DataAttachmentHelper.set(key, entity, updated);
                EyelibNetworkManager.sendToTrackedAndSelf(entity, new ExtraEntityUpdateDataPacket(entity.getId(), updated));
            }
        }
    }
}
