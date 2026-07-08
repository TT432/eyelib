package io.github.tt432.eyelib.bridge.ui.adapter;

import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UITextField;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 包装 MC {@link EditBox} 为 {@link UITextField}。
 *
 * @author TT432
 */
public final class MCTextField implements UITextField {
    private final EditBox editBox;

    public MCTextField(Font font, int x, int y, int w, int h) {
        this.editBox = new EditBox(font, x, y, w, h, Component.empty());
    }

    public EditBox editBox() {
        return editBox;
    }

    @Override
    public String getValue() {
        return editBox.getValue();
    }

    @Override
    public void setValue(String value) {
        editBox.setValue(value);
    }

    @Override
    public void setResponder(Consumer<String> responder) {
        editBox.setResponder(responder::accept);
    }

    @Override
    public void setHint(String text) {
        editBox.setHint(Component.literal(text));
    }

    @Override
    public void setMaxLength(int max) {
        editBox.setMaxLength(max);
    }

    @Override
    public void setBordered(boolean bordered) {
        editBox.setBordered(bordered);
    }

    @Override
    public void setCanLoseFocus(boolean canLoseFocus) {
        editBox.setCanLoseFocus(canLoseFocus);
    }

    @Override
    public void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // EditBox 直接持有 GuiGraphics 引用（render 回调），无法通过 UIGraphics 抽象
        // 这里委托给 MCScreenAdapter 的 addRenderableWidget 路径（MC EditBox 自己渲染）
    }

    @Override
    public void setPosition(int x, int y) {
        editBox.setPosition(x, y);
    }

    @Override
    public int getWidth() {
        return editBox.getWidth();
    }

    @Override
    public int getHeight() {
        return editBox.getHeight();
    }
}

