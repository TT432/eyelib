package io.github.tt432.eyelib.bridge.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIPoseStack;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.Vector2f;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}

/**
 * 将 MC {@code GuiGraphics} + {@link RenderSystem} 适配为 {@link UIGraphics}。
 *
 * @author TT432
 */
public final class MCGraphics implements UIGraphics {
    //? if <26.1 {
    private final GuiGraphics gg;
    private final Font font;
    //?} else {
    private final GuiGraphicsExtractor gg;
    private final net.minecraft.client.gui.Font font;
    //?}

    //? if <26.1 {
    public MCGraphics(GuiGraphics gg) {
        this.gg = gg;
        this.font = Minecraft.getInstance().font;
    }
    //?} else {
    public MCGraphics(GuiGraphicsExtractor gg) {
        this.gg = gg;
        this.font = Minecraft.getInstance().font;
    }
    //?}

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int fontHeight() {
        return font.lineHeight;
    }

    @Override
    public void drawText(String text, int x, int y, int color) {
        gg.drawString(font, text, x, y, color);
    }

    @Override
    public void drawCenteredText(String text, int x, int y, int color) {
        gg.drawCenteredString(font, text, x, y, color);
    }

    @Override
    public void blit(PortResourceLocation texture, int x, int y) {
        blit(texture, x, y, 0, 0, 256, 256);
    }

    @Override
    public void blit(PortResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        //? if <1.20.6 {
        gg.blit(new ResourceLocation(texture.namespace(), texture.path()), x, y, u, v, w, h);
        //?} else {
        gg.blit(ResourceLocation.fromNamespaceAndPath(texture.namespace(), texture.path()), x, y, u, v, w, h);
        //?}
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        gg.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void fillGradient(int x1, int y1, int x2, int y2, int fromColor, int toColor) {
        gg.fillGradient(x1, y1, x2, y2, fromColor, toColor);
    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.gui());
        Vector2f[] points = createRectangleFromLine(new Vector2f(x1, y1), new Vector2f(x2, y2), thickness);
        for (Vector2f point : points) {
            writeVertex(buffer, point, color);
        }
        bufferSource.endBatch(RenderType.gui());
    }

    @Override
    public void enableScissor(int x, int y, int w, int h) {
        gg.enableScissor(x, y, x + w, y + h);
    }

    @Override
    public void disableScissor() {
        gg.disableScissor();
    }

    @Override
    public void enableBlend() {
        RenderSystem.enableBlend();
    }

    @Override
    public void disableBlend() {
        RenderSystem.disableBlend();
    }

    @Override
    public void setShaderColor(float r, float g, float b, float a) {
        RenderSystem.setShaderColor(r, g, b, a);
    }

    @Override
    public void renderTooltip(String text, int x, int y) {
        gg.renderTooltip(font, Component.literal(text), x, y);
    }

    @Override
    public UIPoseStack pose() {
        //? if <26.1 {
        return new MCPoseStack(gg.pose());
        //?} else {
        throw new UnsupportedOperationException("26.1 GuiGraphicsExtractor.pose() not yet adapted");
        //?}
    }

    private static void writeVertex(VertexConsumer buffer, Vector2f point, int color) {
        //? if <1.20.6 {
        buffer.vertex(point.x, point.y, 0).color(color).endVertex();
        //?} else {
        buffer.addVertex(point.x, point.y, 0).setColor(color);
        //?}
    }

    private static Vector2f[] createRectangleFromLine(Vector2f p1, Vector2f p2, float width) {
        Vector2f direction = new Vector2f(p2).sub(p1);
        if (direction.lengthSquared() == 0) {
            return new Vector2f[]{new Vector2f(p1), new Vector2f(p1), new Vector2f(p1), new Vector2f(p1)};
        }

        direction.normalize();
        Vector2f normal = new Vector2f(-direction.y, direction.x).mul(width / 2F);
        return new Vector2f[]{
                new Vector2f(p1).add(normal),
                new Vector2f(p2).add(normal),
                new Vector2f(p2).sub(normal),
                new Vector2f(p1).sub(normal)
        };
    }
}
