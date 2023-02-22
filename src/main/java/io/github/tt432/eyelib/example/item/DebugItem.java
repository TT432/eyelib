package io.github.tt432.eyelib.example.item;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.common.bedrock.BedrockResourceManager;
import io.github.tt432.eyelib.common.bedrock.particle.BedrockParticleManager;
import io.github.tt432.eyelib.common.bedrock.particle.ParticleEmitter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * @author DustW
 */
public class DebugItem extends Item {
    public DebugItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        pInteractionTarget.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        pInteractionTarget.setItemSlot(EquipmentSlot.HEAD,
                new ItemStack(pPlayer.getRandom().nextBoolean() ? Items.IRON_HELMET : Items.DIAMOND_HELMET));
        pInteractionTarget.setItemSlot(EquipmentSlot.CHEST,
                new ItemStack(pPlayer.getRandom().nextBoolean() ? Items.IRON_CHESTPLATE : Items.DIAMOND_CHESTPLATE));
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (pLevel.isClientSide) {
            try {
                BedrockParticleManager.addParticle(ParticleEmitter.from(BedrockResourceManager.getInstance()
                        .getParticle(new ResourceLocation(Eyelib.MOD_ID, "loading")), pLevel, pPlayer.position()));
            } catch (Throwable a) {
                a.printStackTrace();
            }
        }

        return super.use(pLevel, pPlayer, pUsedHand);
    }
}
