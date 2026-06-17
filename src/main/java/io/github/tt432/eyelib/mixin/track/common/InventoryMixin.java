package io.github.tt432.eyelib.mixin.track.common;

import io.github.tt432.eyelib.track.api.ItemTrackApi;
import io.github.tt432.eyelib.track.api.TrackableItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TrackableItem 进入玩家背包时自动分配追踪 ID。
 *
 * @author TT432
 */
@Mixin(Inventory.class)
public class InventoryMixin {

    /**
     * 在 add(int, ItemStack) 入口分配追踪 ID。
     * 只拦截重载版本避免匹配到 add(ItemStack) 委托版本。
     */
    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"))
    private void assignTrackIdOnAdd(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (TrackableItem.isTrackable(stack)
                && ((Inventory) (Object) this).player.level() instanceof ServerLevel serverLevel) {
            ItemTrackApi.getOrAssignId(stack, serverLevel);
        }
    }
}
