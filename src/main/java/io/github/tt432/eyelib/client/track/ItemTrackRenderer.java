package io.github.tt432.eyelib.client.track;

import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.type.MolangString;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * ItemStack 的 eyelib 动画渲染器。
 *
 * @author TT432
 */
/** @author TT432 */
public final class ItemTrackRenderer {

    private ItemTrackRenderer() {
    }

    /**
     * 在渲染前准备 RenderData：绑定 owner、设置 Molang 上下文。
     *
     * @param stack          要渲染的 ItemStack
     * @param displayContext 渲染视角
     * @return 准备好的 RenderData，或 null（如果 item 不支持追踪）
     */
    @Nullable
    public static RenderData<ItemStack> prepareRenderData(ItemStack stack, ItemDisplayContext displayContext) {
        if (!io.github.tt432.eyelibtrack.api.TrackableItem.isTrackable(stack)) {
            return null;
        }

        RenderData<ItemStack> rd = ItemTrackRenderCache.getOrCreateRenderData(stack);
        MolangScope scope = rd.getScope();

        if (scope != null) {
            scope.set("context.item_slot", MolangString.valueOf(displayContext.getSerializedName()));
        }

        return rd;
    }

    /**
     * 获取渲染用的 partialTick。
     */
    public static float getPartialTick() {
        return Minecraft.getInstance().getFrameTime();
    }
}
