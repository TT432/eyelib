package io.github.tt432.eyelib.ui;
import io.github.tt432.eyelib.bridge.ui.adapter.MCGraphics;

import io.github.tt432.eyelib.util.PortResourceLocation;

/**
 * MC 无关的 2D 绘制原语接口。
 * bridge 层 {@code MCGraphics} 包装 GuiGraphics / RenderSystem 提供具体实现。
 *
 * @author TT432
 */
public interface UIGraphics {
    int textWidth(String text);

    int fontHeight();

    void drawText(String text, int x, int y, int color);

    void drawCenteredText(String text, int x, int y, int color);

    void blit(PortResourceLocation texture, int x, int y);

    void blit(PortResourceLocation texture, int x, int y, int u, int v, int w, int h);

    void fill(int x1, int y1, int x2, int y2, int color);

    void fillGradient(int x1, int y1, int x2, int y2, int fromColor, int toColor);

    void drawLine(float x1, float y1, float x2, float y2, float thickness, int color);

    void enableScissor(int x, int y, int w, int h);

    void disableScissor();

    void enableBlend();

    void disableBlend();

    void setShaderColor(float r, float g, float b, float a);

    void renderTooltip(String text, int x, int y);

    UIPoseStack pose();
}

