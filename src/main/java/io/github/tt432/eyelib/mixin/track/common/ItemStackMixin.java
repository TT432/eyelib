package io.github.tt432.eyelib.mixin.track.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.tt432.eyelib.track.api.ItemTrackApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

/**
 * ItemStack 拆分与堆叠比较的 Mixin。
 *
 * @author TT432
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    /**
     * 拆分时移除新堆的追踪 ID，避免多个堆共享同一 ID。
     */
    @WrapOperation(method = "split", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyWithCount(I)Lnet/minecraft/world/item/ItemStack;"))
    public ItemStack removeTrackIdOnSplit(ItemStack instance, int count, Operation<ItemStack> original) {
        ItemStack copy = original.call(instance, count);

        if (count < instance.getCount()) {
            ItemTrackApi.removeTrackId(copy);
        }

        return copy;
    }

    /**
     * 堆叠比较时忽略追踪 ID NBT 标签，避免不同 ID 的同种物品无法堆叠。
     */
    @WrapOperation(method = "isSameItemSameTags", at = @At(value = "INVOKE", target = "Ljava/util/Objects;equals(Ljava/lang/Object;Ljava/lang/Object;)Z"))
    private static boolean removeTrackIdOnTagCompare(Object a, Object b, Operation<Boolean> original) {
        if (original.call(a, b)) {
            return true;
        }

        if (a instanceof CompoundTag tagA && b instanceof CompoundTag tagB) {
            tagA = tagA.copy();
            tagB = tagB.copy();
            tagA.remove("eyelib_track_id");
            tagB.remove("eyelib_track_id");
            return Objects.equals(tagA, tagB);
        }

        return false;
    }
}
