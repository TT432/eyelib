package io.github.tt432.eyelib.mixin;

import io.github.tt432.eyelib.capability.item.EyelibDataComponents;
import io.github.tt432.eyelib.client.gui.tooltip.ReplaceTooltipData;
import io.github.tt432.eyelib.client.loader.ReplacedTooltipLoader;
import io.github.tt432.eyelib.util.ResourceLocations;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author TT432
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Shadow
    private ItemStack tooltipStack;

    @Shadow
    public abstract void blitSprite(ResourceLocation pSprite, int pX, int pY, int pBlitOffset, int pWidth, int pHeight);

    @Inject(method = " lambda$renderTooltipInternal$3(IIIILnet/neoforged/neoforge/client/event/RenderTooltipEvent$Color;)V",
            cancellable = true, at = @At("HEAD"))
    private void eyelib$lambda$renderTooltipInternal$3(int x, int y, int w, int h,
                                                       RenderTooltipEvent.Color colorEvent,
                                                       CallbackInfo ci) {
        ReplaceTooltipData data;

        if (tooltipStack.has(EyelibDataComponents.ITEM_TOOLTIP_REPLACE_DATA)) {
            data = tooltipStack.get(EyelibDataComponents.ITEM_TOOLTIP_REPLACE_DATA).getData();
        } else {
            data = ReplacedTooltipLoader.INSTANCE.getData();
        }

        int i = x - 3;
        int j = y - 3;
        int k = w + 3 + 3;
        int l = h + 3 + 3;

        if (data != null) {
            if (!ResourceLocations.isEmpty(data.texture())) {
                blitSprite(data.texture(), i, j, 400, k, l);
            } else if (!data.color().isEmpty()) {
                ReplaceTooltipData.Color color = data.color();

                TooltipRenderUtil.renderTooltipBackground((GuiGraphics) (Object) this, x, y, w, h, 400,
                        color.backgroundTop().orElse(colorEvent.getBackgroundStart()),
                        color.backgroundBottom().orElse(colorEvent.getBackgroundEnd()),
                        color.borderTop().orElse(colorEvent.getBorderStart()),
                        color.borderBottom().orElse(colorEvent.getBorderEnd()));
            }

            ci.cancel();
        }
    }
}
