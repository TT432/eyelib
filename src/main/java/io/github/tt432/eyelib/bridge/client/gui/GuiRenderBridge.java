package io.github.tt432.eyelib.bridge.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * GUI 渲染状态操作的桥接，封装版本无关的调用接口。
 *
 * @author TT432
 */
public final class GuiRenderBridge {

    private GuiRenderBridge() {}

    public static void enableBlend() {
        RenderSystem.enableBlend();
    }

    public static void disableBlend() {
        RenderSystem.disableBlend();
    }

    public static void setShaderColor(float r, float g, float b, float a) {
        RenderSystem.setShaderColor(r, g, b, a);
    }
}
