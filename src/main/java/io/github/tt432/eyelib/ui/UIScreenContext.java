package io.github.tt432.eyelib.ui;

/**
 * {@link UIScreen#onInit} 时由 bridge 传入的上下文，提供屏幕尺寸、字体度量以及 widget 工厂方法。
 *
 * @author TT432
 */
public interface UIScreenContext {
    int width();

    int height();

    int textWidth(String text);

    int fontHeight();

    UITextField addTextField(int x, int y, int w, int h);

    UIButton addButton(String text, int x, int y, int w, int h, Runnable onClick);

    <T extends UIWidget> T addWidget(T widget);
}
