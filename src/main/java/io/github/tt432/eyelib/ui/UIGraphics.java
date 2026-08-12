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

    void fill(int x1, int y1, int x2, int y2, int color);

    void fillGradient(int x1, int y1, int x2, int y2, int fromColor, int toColor);

    void drawLine(float x1, float y1, float x2, float y2, float thickness, int color);

    /**
     * 填充三角形列表。{@code pts} 为扁平顶点流 {@code [x0,y0,x1,y1,...]}，长度须为 6 的倍数，
     * 每 3 个顶点构成一个三角形。顶点随当前 pose 变换（与 {@link #fill} 一致）。
     */
    void fillTriangles(float[] pts, int color);

    void enableScissor(int x, int y, int w, int h);

    void disableScissor();

    void enableBlend();

    void disableBlend();

    void setShaderColor(float r, float g, float b, float a);

    void renderTooltip(String text, int x, int y);

    UIPoseStack pose();
}

