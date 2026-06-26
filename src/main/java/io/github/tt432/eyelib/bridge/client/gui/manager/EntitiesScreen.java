package io.github.tt432.eyelib.bridge.client.gui.manager;

import io.github.tt432.eyelib.bridge.client.gui.ModalWorksurfaceScreen;
import net.minecraft.client.Minecraft;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
public class EntitiesScreen extends ModalWorksurfaceScreen {
    protected EntitiesScreen() {
        super(Component.empty());
    }

    @Nullable
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
        if (panel != null) {
            panel.onEdited(input);
        }
    }

    //? if <26.1 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (EntitiesListPanel.lastSelected != null) {
            int size = 48;
            EyelibManagerScreen.renderEntityButton(guiGraphics, width - size - border, border, size, 0, EntitiesListPanel.lastSelected);
        }
    }
    //?} else {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphicsExtractor, int mouseX, int mouseY, float partialTick) {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
    }
    //?}

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
