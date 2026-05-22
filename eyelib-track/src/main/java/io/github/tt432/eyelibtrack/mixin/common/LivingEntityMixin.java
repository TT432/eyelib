package io.github.tt432.eyelibtrack.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.tt432.eyelibtrack.api.ItemTrackApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 装备变化检测时恢复追踪 ID 感知。
 *
 * @author TT432
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * 装备变化检测时额外校验追踪 ID，
     * 补偿 {@code isSameItemSameTags} 中忽略追踪 ID 的副作用。
     */
    @WrapOperation(method = "equipmentHasChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    public boolean requireTrackIdForEquipmentSync(ItemStack remoteStack, ItemStack localStack, Operation<Boolean> original) {
        return original.call(remoteStack, localStack) && ItemTrackApi.hasSameTrackId(remoteStack, localStack);
    }
}
