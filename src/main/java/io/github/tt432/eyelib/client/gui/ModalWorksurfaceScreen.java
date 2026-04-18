package io.github.tt432.eyelib.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Consumes mouse interaction like a vanilla container screen so editor-style
 * GUI surfaces do not leak clicks to world/item use behind the screen.
 */
public abstract class ModalWorksurfaceScreen extends Screen {
    protected ModalWorksurfaceScreen(Component title) {
        super(title);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        return true;
    }
}
