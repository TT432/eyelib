package io.github.tt432.eyelib.client.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/**
 * 消耗鼠标交互事件，使编辑器风格的 GUI 界面不会将点击泄露到背后的世界/物品交互。
 *
 * @author TT432
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
