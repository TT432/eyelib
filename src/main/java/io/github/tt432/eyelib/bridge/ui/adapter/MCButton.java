package io.github.tt432.eyelib.bridge.ui.adapter;

import io.github.tt432.eyelib.ui.UIButton;
import io.github.tt432.eyelib.ui.UIGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * 包装 MC {@link Button} 为 {@link UIButton}。
 *
 * @author TT432
 */
public final class MCButton implements UIButton {
    private final Button button;

    public MCButton(String text, int x, int y, int w, int h, Runnable onClick) {
        this.button = Button.builder(Component.literal(text), b -> onClick.run())
                .pos(x, y)
                .size(w, h)
                .build();
    }

    public Button button() {
        return button;
    }

    @Override
    public void setActive(boolean active) {
        button.active = active;
    }

    @Override
    public void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Button 由 MC Screen.addRenderableWidget 直接渲染，render() 为空操作
    }

    @Override
    public void setPosition(int x, int y) {
        button.setPosition(x, y);
    }

    @Override
    public int getWidth() {
        return button.getWidth();
    }

    @Override
    public int getHeight() {
        return button.getHeight();
    }
}

