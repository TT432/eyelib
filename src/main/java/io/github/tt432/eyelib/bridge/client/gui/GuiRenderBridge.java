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
        //? if <26.1 {
        RenderSystem.enableBlend();
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    public static void disableBlend() {
        //? if <26.1 {
        RenderSystem.disableBlend();
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }

    public static void setShaderColor(float r, float g, float b, float a) {
        //? if <26.1 {
        RenderSystem.setShaderColor(r, g, b, a);
        //?} else {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
        //?}
    }
}
