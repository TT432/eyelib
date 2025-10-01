package io.github.tt432.eyelib.client.gui.manager;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.ClientTickHandler;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.function.TriFunction;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author TT432
 */
@RequiredArgsConstructor
final class DragTargetWidget extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final EyelibManagerScreen.GuiAnimator animator;
    @Nullable
    private final ResourceLocation icon;
    private final Component title;
    private final TriFunction<Double, Double, Integer, Boolean> onClicked;

    public boolean hover(double mouseX, double mouseY) {
        return mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        return hover(mouseX, mouseY) && onClicked.apply(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        var a = animator.getTime(ClientTickHandler.getTick(), partialTick, hover(mouseX, mouseY));

        guiGraphics.blit(ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "gui_bg_nine"), x, y, 0, 0, w, h);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, a);
        guiGraphics.blit(ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "gui_bg_nine_selected"), x, y, 0, 0, w, h);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        String titleString = title.getString();
        Font font = Minecraft.getInstance().font;
        int th = font.lineHeight;
        int iconOffset = 0;
        int iconSize = Math.round(h * 0.614F);

        if (icon != null) {
            iconOffset = iconSize / 2;
            guiGraphics.blit(icon, x + w / 2 - iconOffset, y + h / 2 - iconOffset - th / 2, 0, 0, iconSize, iconSize);
        }

        int tw = font.width(titleString);
        guiGraphics.drawString(font, titleString, x + w / 2 - tw / 2, y + h / 2 - th / 2 + iconOffset - th / 4, 0xFFFFFFFF);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
    }
}
