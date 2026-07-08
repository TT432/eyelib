package io.github.tt432.eyelib.bridge.track;

//? if >=1.20.6 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * ItemStack 自定义持久化数据（&lt;1.20.6 NBT tag / &ge;1.20.6 CUSTOM_DATA 组件）的版本差异封装。
 * application 层（如 {@code track/api/ItemTrackApi}）通过本 Port 读写 long 值，
 * 无需感知 tag↔component 边界与 {@code CompoundTag} API（{@code getLong} 返回值类型等）的变化。
 *
 * @author TT432
 */
public interface ItemTrackTagPort {

    /**
     * 读取 ItemStack 自定义数据中存储的 long，不存在时返回 {@code null}。
     */
    @Nullable
    static Long getLongOrNull(ItemStack stack, String key) {
        //? if <1.20.6 {
        CompoundTag tag = stack.getTag();
        //?} else {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : null;
        //?}

        if (tag == null) return null;

        //? if <26.1 {
        if (!tag.contains(key, Tag.TAG_LONG)) return null;
        return tag.getLong(key);
        //?} else {
        var opt = tag.getLong(key);
        return opt.orElse(null);
        //?}
    }

    /**
     * 向 ItemStack 自定义数据写入一个 long（不存在时自动创建容器）。
     */
    static void putLong(ItemStack stack, String key, long value) {
        //? if <1.20.6 {
        stack.getOrCreateTag().putLong(key, value);
        //?} else {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(key, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        //?}
    }

    /**
     * 从 ItemStack 自定义数据移除指定 key；移除后容器为空则一并清理容器。
     */
    static void remove(ItemStack stack, String key) {
        //? if <1.20.6 {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) return;
        tag.remove(key);
        if (tag.isEmpty()) stack.setTag(null);
        //?} else {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(key)) return;
        tag.remove(key);
        if (tag.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        //?}
    }
}
