package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.bridge.client.ClientTickPort;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIWidget;
import io.github.tt432.eyelib.util.PortResourceLocation;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.Nullable;

/**
 * @author TT432
 */
final class DragTargetWidget implements UIWidget {
    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final GuiAnimator animator;
    @Nullable
    private final String icon;
    private final String title;
    private final TriFunction<Double, Double, Integer, Boolean> onClicked;

    DragTargetWidget(int x, int y, int w, int h, GuiAnimator animator, @Nullable String icon,
                     String title, TriFunction<Double, Double, Integer, Boolean> onClicked) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.animator = animator;
        this.icon = icon;
        this.title = title;
        this.onClicked = onClicked;
    }

    public boolean hover(double mouseX, double mouseY) {
        return mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return hover(mouseX, mouseY) && onClicked.apply(mouseX, mouseY, button);
    }

    @Override
    public int getWidth() {
        return w;
    }

    @Override
    public int getHeight() {
        return h;
    }

    @Override
    public void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        var a = animator.getTime(ClientTickPort.getTick(), partialTick, hover(mouseX, mouseY));

        gfx.blit(PortResourceLocation.parse("eyelib:gui_bg_nine"), x, y, 0, 0, w, h);
        gfx.enableBlend();
        gfx.setShaderColor(1, 1, 1, a);
        gfx.blit(PortResourceLocation.parse("eyelib:gui_bg_nine_selected"), x, y, 0, 0, w, h);
        gfx.disableBlend();
        gfx.setShaderColor(1, 1, 1, 1);

        int th = gfx.fontHeight();
        int iconOffset = 0;
        int iconSize = Math.round(h * 0.614F);

        if (icon != null) {
            iconOffset = iconSize / 2;
            gfx.blit(PortResourceLocation.parse(icon), x + w / 2 - iconOffset, y + h / 2 - iconOffset - th / 2, 0, 0, iconSize, iconSize);
        }

        int tw = gfx.textWidth(title);
        gfx.drawText(title, x + w / 2 - tw / 2, y + h / 2 - th / 2 + iconOffset - th / 4, 0xFFFFFFFF);
    }
}
