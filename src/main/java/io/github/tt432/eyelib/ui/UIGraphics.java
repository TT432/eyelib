package io.github.tt432.eyelib.ui;

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

    /**
     * 整张纹理拉伸绘制到 w×h 区域。调用方提供纹理像素尺寸用于 UV 归一化
     * （1.20.1 无 GUI sprite atlas，无法自动获知纹理尺寸）。
     */
    void blitScaled(PortResourceLocation texture, int x, int y, int w, int h, int texW, int texH);

    /** 九宫格拉伸：四角保持原样，边/中心拉伸。border 为四边等宽像素。 */
    void blitNineSlice(PortResourceLocation texture, int x, int y, int w, int h, int texW, int texH, int border);

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

