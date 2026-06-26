package io.github.tt432.eyelib.bridge.ui;

import io.github.tt432.eyelib.ui.UIButton;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UITextField;
import io.github.tt432.eyelib.ui.UIWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * 将 MC {@link Screen} 事件翻译为 {@link UIScreen} 调用。
 * 版本签名差异在本类内用 {@code //?} 消化，UIScreen 实现类无需关心。
 *
 * @author TT432
 */
public final class MCScreenAdapter extends Screen implements UIScreenContext {
    private final UIScreen screen;

    public MCScreenAdapter(UIScreen screen) {
        super(Component.empty());
        this.screen = screen;
    }

    public static Screen wrap(UIScreen screen) {
        return new MCScreenAdapter(screen);
    }

    @Override
    protected void init() {
        super.init();
        screen.onInit(this);
    }

    // ---- UIScreenContext ----

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return this.height;
    }

    @Override
    public int textWidth(String text) {
        return this.font.width(text);
    }

    @Override
    public int fontHeight() {
        return this.font.lineHeight;
    }

    @Override
    public UITextField addTextField(int x, int y, int w, int h) {
        MCTextField field = new MCTextField(this.font, x, y, w, h);
        addRenderableWidget(field.editBox());
        return field;
    }

    @Override
    public UIButton addButton(String text, int x, int y, int w, int h, Runnable onClick) {
        MCButton button = new MCButton(text, x, y, w, h, onClick);
        addRenderableWidget(button.button());
        return button;
    }

    @Override
    public <T extends UIWidget> T addWidget(T widget) {
        addRenderableWidget(MCWidgetAdapter.wrap(widget));
        return widget;
    }

    //? if <26.1 {
    @Override
    public void render(net.minecraft.client.gui.GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        screen.onRender(new MCGraphics(gg), mouseX, mouseY, partialTick);
        super.render(gg, mouseX, mouseY, partialTick);
    }
    //?} else {
    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor gg, int mouseX, int mouseY, float partialTick) {
        screen.onRender(new MCGraphics(gg), mouseX, mouseY, partialTick);
        super.extractRenderState(gg, mouseX, mouseY, partialTick);
    }
    //?}

    //? if <26.1 {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return screen.onKeyPress(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }
    //?} else {
    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        return screen.onKeyPress(event.key(), event.scanCode(), event.modifiers()) || super.keyPressed(event);
    }
    //?}

    //? if <26.1 {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return screen.onMouseClick(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return screen.onMouseRelease(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return screen.onMouseDrag(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    //?} else {
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return screen.onMouseClick(event.mouseX(), event.mouseY(), event.button()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        return screen.onMouseRelease(event.mouseX(), event.mouseY(), event.button()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        return screen.onMouseDrag(event.mouseX(), event.mouseY(), event.button(), dragX, dragY) || super.mouseDragged(event, dragX, dragY);
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
        return screen.onMouseScroll(mouseX, mouseY, delta) || super.mouseScrolled(mouseX, mouseY, delta);
        //?} else {
        return screen.onMouseScroll(mouseX, mouseY, scrollDelta) || super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDelta);
        //?}
    }

    @Override
    public void onFilesDrop(List<Path> files) {
        screen.onFilesDrop(files);
    }

    @Override
    public void onClose() {
        screen.onClose();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return screen.isPauseScreen();
    }

    @Override
    public void tick() {
        screen.onTick();
    }
}
