package io.github.tt432.eyelib.bridge.ui.adapter;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIPoseStack;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
//? if <26.1 {
import net.minecraft.client.renderer.RenderType;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
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
        //? if <26.1 {
        gg.drawString(font, text, x, y, color);
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    @Override
    public void drawCenteredText(String text, int x, int y, int color) {
        //? if <26.1 {
        gg.drawCenteredString(font, text, x, y, color);
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    @Override
    public void blit(PortResourceLocation texture, int x, int y) {
        blit(texture, x, y, 0, 0, 256, 256);
    }

    @Override
    public void blit(PortResourceLocation texture, int x, int y, int u, int v, int w, int h) {
        //? if <26.1 {
        //? if <1.20.6 {
        gg.blit(new ResourceLocation(texture.namespace(), texture.path()), x, y, u, v, w, h);
        //?} else {
        gg.blit(ResourceLocation.fromNamespaceAndPath(texture.namespace(), texture.path()), x, y, u, v, w, h);
        //?}
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
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
        //? if <26.1 {
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.gui());
        Vector2f[] points = createRectangleFromLine(new Vector2f(x1, y1), new Vector2f(x2, y2), thickness);
        for (Vector2f point : points) {
            writeVertex(buffer, point, color);
        }
        bufferSource.endBatch(RenderType.gui());
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    @Override
    public void fillTriangles(float[] pts, int color) {
        if (pts.length % 6 != 0) {
            throw new IllegalArgumentException("pts length must be a multiple of 6, got " + pts.length);
        }
        //? if <26.1 {
        if (pts.length == 0) {
            return;
        }
        org.joml.Matrix4f pose = gg.pose().last().pose();
        // guiOverlay()：NO_DEPTH_TEST + COLOR_WRITE，与文字同语义（painter 顺序覆盖，
        // 不与世界/GUI 深度交互）。每三角形写为退化 quad (v0,v2,v1,v1) 对齐 QUADS mode
        // 与 gui 系正面环绕（屏幕逆时针）。末尾 endBatch 立即 flush。
        float a = ((color >>> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.guiOverlay());
        for (int i = 0; i < pts.length; i += 6) {
            //? if <1.20.6 {
            buffer.vertex(pose, pts[i], pts[i + 1], 0).color(r, g, b, a).endVertex();
            buffer.vertex(pose, pts[i + 4], pts[i + 5], 0).color(r, g, b, a).endVertex();
            buffer.vertex(pose, pts[i + 2], pts[i + 3], 0).color(r, g, b, a).endVertex();
            buffer.vertex(pose, pts[i + 2], pts[i + 3], 0).color(r, g, b, a).endVertex();
            //?} else {
            buffer.addVertex(pose, pts[i], pts[i + 1], 0).setColor(r, g, b, a);
            buffer.addVertex(pose, pts[i + 4], pts[i + 5], 0).setColor(r, g, b, a);
            buffer.addVertex(pose, pts[i + 2], pts[i + 3], 0).setColor(r, g, b, a);
            buffer.addVertex(pose, pts[i + 2], pts[i + 3], 0).setColor(r, g, b, a);
            //?}
        }
        bufferSource.endBatch(RenderType.guiOverlay());
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
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
        //? if <26.1 {
        RenderSystem.enableBlend();
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    @Override
    public void disableBlend() {
        //? if <26.1 {
        RenderSystem.disableBlend();
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    @Override
    public void setShaderColor(float r, float g, float b, float a) {
        //? if <26.1 {
        RenderSystem.setShaderColor(r, g, b, a);
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    @Override
    public void renderTooltip(String text, int x, int y) {
        //? if <26.1 {
        gg.renderTooltip(font, Component.literal(text), x, y);
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
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
        float a = ((color >>> 24) & 0xFF) / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        //? if <1.20.6 {
        buffer.vertex(point.x, point.y, 0).color(r, g, b, a).endVertex();
        //?} else {
        buffer.addVertex(point.x, point.y, 0).setColor(r, g, b, a);
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

