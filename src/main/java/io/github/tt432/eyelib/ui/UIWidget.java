package io.github.tt432.eyelib.ui;

/**
 * MC 无关的 widget 接口，由 bridge {@code MCWidgetAdapter} 包装为 MC Renderable + GuiEventListener。
 *
 * @author TT432
 */
public interface UIWidget {
    void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    default void setPosition(int x, int y) {}

    default int getWidth() {
        return 0;
    }

    default int getHeight() {
        return 0;
    }
}
