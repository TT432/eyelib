package io.github.tt432.eyelib.bridge.ui;

import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * 将 application {@link UIWidget} 适配为 MC {@link AbstractWidget}，供 Screen.addRenderableWidget 使用。
 *
 * @author TT432
 */
public final class MCWidgetAdapter extends AbstractWidget {
    private final UIWidget widget;

    private MCWidgetAdapter(UIWidget widget, int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.widget = widget;
    }

    public static MCWidgetAdapter wrap(UIWidget widget) {
        return new MCWidgetAdapter(widget, 0, 0, widget.getWidth(), widget.getHeight());
    }

    //? if <26.1 {
    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        widget.render(new MCGraphics(gg), mouseX, mouseY, partialTick);
    }
    //?} else {
    @Override
    protected void extractWidgetRenderState(net.minecraft.client.gui.GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
    }
    //?}

    //? if <26.1 {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return widget.mouseClicked(mouseX, mouseY, button);
    }
    //?} else {
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return widget.mouseClicked(event.x(), event.y(), event.button());
    }
    //?}

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 //? if <1.20.6 {
                                 double delta
                                 //?} else {
                                 double scrollDeltaX, double scrollDelta
                                 //?}
    ) {
        //? if <1.20.6 {
        return widget.mouseScrolled(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
        //?} else {
        return widget.mouseScrolled(mouseX, mouseY, scrollDelta) || super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDelta);
        //?}
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    @Override
    public void setPosition(int x, int y) {
        widget.setPosition(x, y);
        super.setPosition(x, y);
    }
}
