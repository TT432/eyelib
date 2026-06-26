package io.github.tt432.clientsmoke.runtime;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;

/**
 * 为构造函数式 smoke test 提供一次性渲染钩子。
 *
 * @author TT432
 */
public final class ClientSmokeVisualHooks {

    @FunctionalInterface
    public interface RenderHook {
        void render(Minecraft mc) throws Exception;
    }

    @FunctionalInterface
    public interface CaptureVerifier {
        void verify(NativeImage image) throws Exception;
    }

    private static RenderHook renderHook;
    private static CaptureVerifier captureVerifier;

    public static void set(RenderHook renderHook, CaptureVerifier captureVerifier) {
        ClientSmokeVisualHooks.renderHook = renderHook;
        ClientSmokeVisualHooks.captureVerifier = captureVerifier;
    }

    static void renderBeforeCapture(Minecraft mc) throws Exception {
        if (renderHook != null) {
            renderHook.render(mc);
        }
    }

    static void verifyCapture(NativeImage image) throws Exception {
        if (captureVerifier != null) {
            captureVerifier.verify(image);
        }
        clear();
    }

    static void clear() {
        renderHook = null;
        captureVerifier = null;
    }

    private ClientSmokeVisualHooks() {}
}
