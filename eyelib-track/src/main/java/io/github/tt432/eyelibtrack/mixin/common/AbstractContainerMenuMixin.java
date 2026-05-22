package io.github.tt432.eyelibtrack.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.tt432.eyelibtrack.api.ItemTrackApi;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 容器操作的 Mixin：克隆时清理追踪 ID，槽位变化检测时恢复 ID 感知。
 *
 * @author TT432
 */
@Mixin(AbstractContainerMenu.class)
public class AbstractContainerMenuMixin {

    /**
     * 创造模式中键克隆时移除新堆的追踪 ID。
     */
    @WrapOperation(method = "doClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;", ordinal = 1))
    public ItemStack removeTrackIdOnClone(ItemStack instance, int count, Operation<ItemStack> original) {
        ItemStack copy = original.call(instance, count);
        ItemTrackApi.removeTrackId(copy);
        return copy;
    }

    /**
     * 槽位变化检测时额外校验追踪 ID 是否一致，
     * 补偿 {@code isSameItemSameTags} 中忽略追踪 ID 的副作用。
     */
    @WrapOperation(method = "triggerSlotListeners", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    public boolean requireTrackIdForSlotSync(ItemStack stack, ItemStack other, Operation<Boolean> original) {
        return original.call(stack, other) && ItemTrackApi.hasSameTrackId(stack, other);
    }
}
