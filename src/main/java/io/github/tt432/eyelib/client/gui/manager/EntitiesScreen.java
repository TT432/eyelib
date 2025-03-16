package io.github.tt432.eyelib.client.gui.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * @author TT432
 */
public class EntitiesScreen extends Screen {
    protected EntitiesScreen() {
        super(Component.empty());
    }

    EntitiesListPanel panel;
    int border;

    @Override
    protected void init() {
        border = Math.round(height * 0.1F);
        int inputHeight = Math.round(Minecraft.getInstance().font.lineHeight / 0.614F);
        int leftAreaWidth = 120;
        EditBox input = new EditBox(this.minecraft.fontFilterFishy, border, border, leftAreaWidth, inputHeight, Component.empty());
        input.setMaxLength(256);
        input.setBordered(true);
        input.setResponder(this::onEdited);
        input.setCanLoseFocus(true);
        this.addRenderableWidget(input);
        int padding = Math.round(inputHeight * .1F);
        addRenderableWidget(panel = new EntitiesListPanel(Minecraft.getInstance(), leftAreaWidth, Math.round(height * 0.8F) - (inputHeight + padding), border + (inputHeight + padding), border));
    }

    void onEdited(String input) {
        panel.onEdited(input);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (EntitiesListPanel.lastSelected != null) {
            int size = 48;
            EyelibManagerScreen.renderEntityButton(guiGraphics, width - size - border, border, size, 0, EntitiesListPanel.lastSelected);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
