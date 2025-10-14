package io.github.tt432.eyelib.mixin.attachable;

import io.github.tt432.eyelib.client.AttchableHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author TT432
 */
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    @Inject(method = "renderSlot", at = @At("HEAD"))
    private void eyelib$renderSlotStart(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AttchableHandler.setRenderingScreen((AbstractContainerScreen<?>) (Object) this, slot);
    }
    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void eyelib$renderSlotEnd(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        AttchableHandler.clearRenderingScreen();
    }
}
