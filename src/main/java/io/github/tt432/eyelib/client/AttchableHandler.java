package io.github.tt432.eyelib.client;

import io.github.tt432.eyelib.capability.RenderData;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;

/**
 * @author TT432
 */
public class AttchableHandler {
    // screen
    private static AbstractContainerScreen<?> renderingScreen;
    private static Slot renderingSlot;

    private static final Int2ReferenceOpenHashMap<RenderData<ItemStack>> dataMap = new Int2ReferenceOpenHashMap<>();

    // entity
    private static LivingEntity renderingEntity;
    private static InteractionHand rendingHand;

    private static final RenderData<ItemStack> mainHandRenderData = new RenderData<>();
    private static final RenderData<ItemStack> offHandRenderData = new RenderData<>();

    public static void setRenderingScreen(AbstractContainerScreen<?> menu, Slot slot) {
        renderingScreen = menu;
        renderingSlot = slot;
    }

    public static void clearRenderingScreen() {
        renderingScreen = null;
        renderingSlot = null;
    }

    public static void setRenderingHandItem(LivingEntity renderingEntity, InteractionHand rendingHand, ItemStack item) {
        AttchableHandler.renderingEntity = renderingEntity;
        AttchableHandler.rendingHand = rendingHand;
    }

    public static void clearRenderingHandItem() {
        AttchableHandler.renderingEntity = null;
        AttchableHandler.rendingHand = null;
    }

    @EventBusSubscriber(Dist.CLIENT)
    public static class ScreenEvents {
        @SubscribeEvent
        public static void onEvent(ScreenEvent.Closing event) {
            dataMap.clear();
        }
    }

    @Nullable
    public static RenderData<ItemStack> getRendingItemStackRenderData(ItemStack stack) {
        if (renderingScreen != null && renderingSlot != null) {
            return dataMap.computeIfAbsent(renderingSlot.index, i -> {
                var result = new RenderData<ItemStack>();
                if (result.getOwner() != stack)
                    result.init(stack);
                return result;
            });
        } else if (getRenderingEntity() != null && rendingHand != null) {
            var result = switch (rendingHand) {
                case MAIN_HAND -> mainHandRenderData;
                case OFF_HAND -> offHandRenderData;
            };
            if (result.getOwner() != stack)
                result.init(stack);
            return result;
        } else {
            return null;
        }
    }

    @Nullable
    public static LivingEntity getRenderingEntity() {
        return renderingEntity == null ? Minecraft.getInstance().player : renderingEntity;
    }
}
