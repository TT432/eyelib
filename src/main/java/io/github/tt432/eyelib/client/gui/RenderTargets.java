package io.github.tt432.eyelib.client.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * @author TT432
 */
public class RenderTargets {
    public static final VarHandle colorTextureIdHandle;
    public static final VarHandle depthBufferIdHandle;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(RenderTarget.class, MethodHandles.lookup());
            colorTextureIdHandle = lookup.findVarHandle(RenderTarget.class, "colorTextureId", int.class);
            depthBufferIdHandle = lookup.findVarHandle(RenderTarget.class, "depthBufferId", int.class);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void swap(RenderTarget a, RenderTarget b) {
        var width = a.width;
        var height = a.height;
        var viewWidth = a.viewWidth;
        var viewHeight = a.viewHeight;
        var frameBufferId = a.frameBufferId;
        var colorTextureId = colorTextureIdHandle.get(a);
        var depthBufferId = depthBufferIdHandle.get(a);

        a.width = b.width;
        a.height = b.height;
        a.viewWidth = b.viewWidth;
        a.viewHeight = b.viewHeight;
        a.frameBufferId = b.frameBufferId;
        colorTextureIdHandle.set(a, colorTextureIdHandle.get(b));
        depthBufferIdHandle.set(a, depthBufferIdHandle.get(b));

        b.width = width;
        b.height = height;
        b.viewWidth = viewWidth;
        b.viewHeight = viewHeight;
        b.frameBufferId = frameBufferId;
        colorTextureIdHandle.set(b, colorTextureId);
        depthBufferIdHandle.set(b, depthBufferId);
    }
}
