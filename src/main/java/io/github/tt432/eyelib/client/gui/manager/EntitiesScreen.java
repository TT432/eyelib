package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import org.jspecify.annotations.Nullable;

/**
 * 客户端实体选择屏幕。
 *
 * @author TT432
 */
public final class EntitiesScreen implements UIScreen {
    @Nullable
    private UIScreenContext ctx;

    @Nullable
    private EntitiesListPanel panel;

    private int border;

    @Override
    public void onInit(UIScreenContext ctx) {
        this.ctx = ctx;
        border = Math.round(ctx.height() * 0.1F);
        int inputHeight = Math.round(ctx.fontHeight() / 0.614F);
        int leftAreaWidth = 120;
        var input = ctx.addTextField(border, border, leftAreaWidth, inputHeight);
        input.setMaxLength(256);
        input.setBordered(true);
        input.setResponder(this::onEdited);
        input.setCanLoseFocus(true);
        int padding = Math.round(inputHeight * .1F);
        panel = ctx.addWidget(new EntitiesListPanel(
                border,
                border + inputHeight + padding,
                leftAreaWidth,
                Math.round(ctx.height() * 0.8F) - (inputHeight + padding)
        ));
    }

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UIScreenContext currentCtx = ctx;
        EntityButton selected = EntitiesListPanel.lastSelected;
        if (currentCtx != null && selected != null) {
            int size = 48;
            EntityButtonRenderer.render(gfx, currentCtx.width() - size - border, border, size, 0, selected);
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        return panel != null && panel.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        return panel != null && panel.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void onEdited(String input) {
        if (panel != null) {
            panel.onEdited(input);
        }
    }
}
