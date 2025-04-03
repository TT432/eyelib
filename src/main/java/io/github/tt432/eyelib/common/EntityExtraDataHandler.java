package io.github.tt432.eyelib.common;

import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.network.ExtraEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * @author TT432
 */
@EventBusSubscriber
public class EntityExtraDataHandler {

    @SubscribeEvent
    public static void onEvent(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new ExtraEntityDataPacket(
                    event.getTarget().getId(),
                    event.getTarget().getData(EyelibAttachableData.EXTRA_ENTITY_DATA)
            ));
        }
    }

    @SubscribeEvent
    public static void onEvent(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            switch (event.getEntity()) {
                case Zombie z -> {
                    ExtraEntityData oldData = z.getData(EyelibAttachableData.EXTRA_ENTITY_DATA);
                    if (oldData.variant() == -1) {
                        ExtraEntityData data = oldData.withVariant(z.getRandom().nextInt(3));
                        z.setData(EyelibAttachableData.EXTRA_ENTITY_DATA, data);
                    }
                }
                case Llama llama -> {
                    ExtraEntityData data = llama.getData(EyelibAttachableData.EXTRA_ENTITY_DATA);
                    ExtraEntityData newData = data.withVariant(llama.getVariant().getId());

                    if (llama instanceof TraderLlama) {
                        newData = newData.withMark_variant(1);
                    }

                    llama.setData(EyelibAttachableData.EXTRA_ENTITY_DATA, newData);
                }
                case Fox fox -> {
                    ExtraEntityData data = fox.getData(EyelibAttachableData.EXTRA_ENTITY_DATA);

                    if (data.variant() == -1) {
                        ExtraEntityData newData;
                        if (fox.getVariant() == Fox.Type.SNOW) {
                            newData = data.withVariant(switch (weightedRandom(fox.getRandom(), new int[]{5, 1, 1, 1})) {
                                case 0 -> 1;
                                case 1 -> 9;
                                case 2 -> 10;
                                case 3 -> 11;
                                default -> -1;
                            });
                        } else {
                            newData = data.withVariant(switch (weightedRandom(fox.getRandom(), new int[]{5, 1, 1, 1, 1, 1, 1, 1})) {
                                case 0 -> 0;
                                case 1 -> 6;
                                case 2 -> 2;
                                case 3 -> 3;
                                case 4 -> 4;
                                case 5 -> 5;
                                case 6 -> 8;
                                case 7 -> 7;
                                default -> -1;
                            });
                        }

                        fox.setData(EyelibAttachableData.EXTRA_ENTITY_DATA, newData);
                    }
                }
                case Chicken chicken -> {
                    ExtraEntityData oldData = chicken.getData(EyelibAttachableData.EXTRA_ENTITY_DATA);
                    if (oldData.variant() == -1) {
                        ExtraEntityData data = oldData.withVariant(chicken.getRandom().nextInt(3));
                        chicken.setData(EyelibAttachableData.EXTRA_ENTITY_DATA, data);
                    }
                }
                default -> {
                }
            }
        }
    }

    private static int weightedRandom(RandomSource source, int[] weights) {
        int totalWeight = 0;
        for (int weight : weights) {
            totalWeight += weight;
        }
        int randomValue = source.nextInt(totalWeight);
        int cumulativeWeight = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulativeWeight += weights[i];
            if (randomValue < cumulativeWeight) {
                return i;
            }
        }
        return -1;
    }

    @SubscribeEvent
    public static void onEvent(EntityTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide) {
            if (event.getEntity() instanceof Mob r) {
                ExtraEntityData data = event.getEntity().getData(EyelibAttachableData.EXTRA_ENTITY_DATA);
                ExtraEntityData oldData = data;
                var facing_target_to_range_attack = false;
                var is_avoiding_mobs = false;
                var is_avoid = false;
                var is_grazing = false;

                for (WrappedGoal availableGoal : r.goalSelector.getAvailableGoals()) {
                    if (availableGoal.isRunning() &&
                            ((availableGoal.getGoal() instanceof RangedAttackGoal rag && rag.attackTime > 0)
                                    || (availableGoal.getGoal() instanceof RangedBowAttackGoal && r.getTicksUsingItem() < 19))
                    ) {
                        facing_target_to_range_attack = true;
                    }
                    if (availableGoal.getGoal() instanceof AvoidEntityGoal<?> avoid) {
                        if (avoid.toAvoid instanceof Mob)
                            is_avoiding_mobs = true;
                        if (avoid.toAvoid != null)
                            is_avoid = true;
                    }
                    if (availableGoal.getGoal() instanceof EatBlockGoal && availableGoal.isRunning()) {
                        is_grazing = true;
                    }
                }

                if (data.facing_target_to_range_attack() != facing_target_to_range_attack)
                    data = data.withFacing_target_to_range_attack(facing_target_to_range_attack);
                if (data.is_avoiding_mobs() != is_avoiding_mobs)
                    data = data.with_avoiding_mobs(is_avoiding_mobs);
                if (data.is_grazing() != is_grazing)
                    data = data.with_grazing(is_grazing);
                if (data.is_avoid() != is_avoid)
                    data = data.with_avoid(is_avoid);

                event.getEntity().setData(EyelibAttachableData.EXTRA_ENTITY_DATA, data);

                if (oldData != data)
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(r, new ExtraEntityDataPacket(r.getId(), data));
            }
        }
    }
}
