package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
//? if <1.20.6 {
import net.minecraftforge.registries.ForgeRegistries;
//?}
import org.jspecify.annotations.Nullable;

/**
 * 根据实体手持物品查找匹配的 attachable 定义。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableResolver {
    @Nullable
    public static BrClientEntity resolve(LivingEntity holder, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        //? if <1.20.6 {
        var itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        //?} else {
        var itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        //?}
        if (itemKey == null) {
            return null;
        }

        return resolveByItemId(itemKey.toString());
    }

    @Nullable
    static BrClientEntity resolveByItemId(String itemId) {
        for (BrClientEntity attachable : AttachableManager.INSTANCE.getAllData().values()) {
            if (attachable.item().containsKey(itemId)) {
                return attachable;
            }
        }
        return null;
    }
}
