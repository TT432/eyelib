package io.github.tt432.eyelib.bridge.client.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * @author TT432
 */
public class RenderTargets {
    public static final VarHandle colorTextureIdHandle;
    public static final VarHandle depthBufferIdHandle;
//? if >=26.1 {
    public static final VarHandle widthHandle;
    public static final VarHandle heightHandle;
    public static final VarHandle viewWidthHandle;
    public static final VarHandle viewHeightHandle;
    public static final VarHandle frameBufferIdHandle;
//?}

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(RenderTarget.class, MethodHandles.lookup());
            colorTextureIdHandle = lookup.findVarHandle(RenderTarget.class, "colorTextureId", int.class);
            depthBufferIdHandle = lookup.findVarHandle(RenderTarget.class, "depthBufferId", int.class);
//? if >=26.1 {
            widthHandle = lookup.findVarHandle(RenderTarget.class, "width", int.class);
            heightHandle = lookup.findVarHandle(RenderTarget.class, "height", int.class);
            viewWidthHandle = lookup.findVarHandle(RenderTarget.class, "viewWidth", int.class);
            viewHeightHandle = lookup.findVarHandle(RenderTarget.class, "viewHeight", int.class);
            frameBufferIdHandle = lookup.findVarHandle(RenderTarget.class, "frameBufferId", int.class);
//?}
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void swap(RenderTarget a, RenderTarget b) {
//? if <26.1 {
        var width = a.width;
        var height = a.height;
        var viewWidth = a.viewWidth;
        var viewHeight = a.viewHeight;
        var frameBufferId = a.frameBufferId;
//?} else {
        var width = widthHandle.get(a);
        var height = heightHandle.get(a);
        var viewWidth = viewWidthHandle.get(a);
        var viewHeight = viewHeightHandle.get(a);
        var frameBufferId = frameBufferIdHandle.get(a);
//?}
        var colorTextureId = colorTextureIdHandle.get(a);
        var depthBufferId = depthBufferIdHandle.get(a);

//? if <26.1 {
        a.width = b.width;
        a.height = b.height;
        a.viewWidth = b.viewWidth;
        a.viewHeight = b.viewHeight;
        a.frameBufferId = b.frameBufferId;
//?} else {
        widthHandle.set(a, widthHandle.get(b));
        heightHandle.set(a, heightHandle.get(b));
        viewWidthHandle.set(a, viewWidthHandle.get(b));
        viewHeightHandle.set(a, viewHeightHandle.get(b));
        frameBufferIdHandle.set(a, frameBufferIdHandle.get(b));
//?}
        colorTextureIdHandle.set(a, colorTextureIdHandle.get(b));
        depthBufferIdHandle.set(a, depthBufferIdHandle.get(b));

//? if <26.1 {
        b.width = width;
        b.height = height;
        b.viewWidth = viewWidth;
        b.viewHeight = viewHeight;
        b.frameBufferId = frameBufferId;
//?} else {
        widthHandle.set(b, width);
        heightHandle.set(b, height);
        viewWidthHandle.set(b, viewWidth);
        viewHeightHandle.set(b, viewHeight);
        frameBufferIdHandle.set(b, frameBufferId);
//?}
        colorTextureIdHandle.set(b, colorTextureId);
        depthBufferIdHandle.set(b, depthBufferId);
    }
}