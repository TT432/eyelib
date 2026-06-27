package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.bridge.client.entity.ItemKeyResolver;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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

        //? if <26.1 {
        ResourceLocation itemKey = ItemKeyResolver.getItemKey(stack);
        //?} else {
        Identifier itemKey = ItemKeyResolver.getItemKey(stack);
        //?}
        if (itemKey == null) {
            return null;
        }

        return resolveByItemId(itemKey.toString());
    }

    @Nullable
    public static BrClientEntity resolveByItemId(String itemId) {
        for (BrClientEntity attachable : AttachableManager.INSTANCE.getAllData().values()) {
            if (attachable.item().containsKey(itemId)) {
                return attachable;
            }
        }
        return null;
    }
}
