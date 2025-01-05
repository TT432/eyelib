package io.github.tt432.eyelib.common;

import io.github.tt432.eyelib.capability.ExtraEntityData;
import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.network.ExtraEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;

/**
 * @author TT432
 */
@EventBusSubscriber
public class EntityExtraDataHandler {
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
        if (!event.getLevel().isClientSide) {
            switch (event.getEntity()) {
                case Zombie z -> {
                    ExtraEntityData oldData = z.getData(EyelibAttachableData.EXTRA_ENTITY_DATA);
                    if (oldData.variant() == -1) {
                        ExtraEntityData data = oldData.withVariant(z.getRandom().nextInt(3));
                        z.setData(EyelibAttachableData.EXTRA_ENTITY_DATA, data);
                        queue2.add(() -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(z, new ExtraEntityDataPacket(z.getId(), data)));
                    }
                }
                case Llama llama -> {
                    ExtraEntityData data = llama.getData(EyelibAttachableData.EXTRA_ENTITY_DATA);
                    ExtraEntityData newData = data.withVariant(llama.getVariant().getId());
                    llama.setData(EyelibAttachableData.EXTRA_ENTITY_DATA, newData);
                    queue2.add(() -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(llama, new ExtraEntityDataPacket(llama.getId(), newData)));
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
                        queue2.add(() -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(fox, new ExtraEntityDataPacket(fox.getId(), newData)));
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
