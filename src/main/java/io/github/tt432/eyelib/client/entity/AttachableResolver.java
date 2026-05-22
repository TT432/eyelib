package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * 根据实体手持物品查找匹配的 attachable 定义。
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class AttachableResolver {
    @Nullable
    public static BrClientEntity resolve(LivingEntity holder, ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        var itemKey = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemKey == null) {
            return null;
        }

        return resolveByItemId(itemKey.toString());
    }

    @Nullable
    static BrClientEntity resolveByItemId(String itemId) {
        for (BrClientEntity attachable : AttachableManager.readPort().getAllData().values()) {
            if (attachable.item().containsKey(itemId)) {
                return attachable;
            }
        }
        return null;
    }
}
