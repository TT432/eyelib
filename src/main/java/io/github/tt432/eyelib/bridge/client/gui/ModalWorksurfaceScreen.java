package io.github.tt432.eyelib.bridge.client.gui;

//? if >=26.1 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
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

    //? if <26.1 {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        return true;
    }
    //?} else {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        super.mouseClicked(event, doubleClick);
        return true;
    }
    //?}

    //? if <26.1 {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        return true;
    }
    //?} else {
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        super.mouseReleased(event);
        return true;
    }
    //?}
}
