package io.github.tt432.eyelib.bridge.client.entity;

import org.jspecify.annotations.Nullable;

//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.item.ItemStack;
//? if <1.20.6 {
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
import net.minecraft.core.registries.BuiltInRegistries;
//?}

/**
 * item registry 查询的版本差异封装。
 *
 * @author TT432
 */
public interface ItemKeyResolver {
    /**
     * @return 给定物品栈在所属注册表中的 key，不存在时为 null
     */
    //? if <26.1 {
    public static ResourceLocation getItemKey(ItemStack stack) {
    //?} else {
    public static Identifier getItemKey(ItemStack stack) {
    //?}
        //? if <1.20.6 {
        return ForgeRegistries.ITEMS.getKey(stack.getItem());
        //?} else {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
        //?}
    }

    /**
     * @return 给定物品栈在所属注册表中的 key 的字符串形式（{@code "namespace:path"}），不存在时为 null。
     * 版本无关的稳定 API，供 application 层使用。
     */

    public static @Nullable String getItemKeyString(ItemStack stack) {
        var key = getItemKey(stack);
        return key == null ? null : key.toString();
    }
}
