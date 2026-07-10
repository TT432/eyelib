package io.github.tt432.eyelib.smoke;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.clientsmoke.runtime.ClientSmokeVisualHooks;
import io.github.tt432.clientsmoke.runtime.EntitySceneRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class EntityCaptureBase {
    static final int FBO_SIZE = 512;
    static final int BG_ARGB = 0xFFD0D0D8;
    private static final Logger LOG = LoggerFactory.getLogger("EntityCapture");

    private static final float ORIGIN_Y = FBO_SIZE * 0.75F;
    private static final float ORIGIN_X = FBO_SIZE * 0.5F;

    private EntityCaptureBase() {}

    static void captureSingle(Minecraft mc, EntityType<?> factory,
                              float scale, float yaw, String name) {
        if (mc.level == null) {
            throw new RuntimeException("Level not available");
        }

        Entity entity = factory.create(mc.level);
        if (entity == null) {
            throw new RuntimeException("Failed to create entity: " + factory);
        }
        entity.setPos(0, 0, 0);
        entity.tickCount = 10;

        ClientSmokeVisualHooks.setScene(FBO_SIZE, FBO_SIZE, (m, rt) -> {
            EntitySceneRenderer.beginScene(m, rt, BG_ARGB);
            EntitySceneRenderer.renderEntityAt(m, entity, ORIGIN_X, ORIGIN_Y, scale, yaw);
            EntitySceneRenderer.endScene(m);
        }, image -> {
            verifyNonEmpty(image, name);
        });
    }

    private static void verifyNonEmpty(NativeImage image, String name) {
        int bgR = (BG_ARGB >> 16) & 0xFF;
        int bgG = (BG_ARGB >> 8) & 0xFF;
        int bgB = BG_ARGB & 0xFF;
        int threshold = 15;

        int nonBg = 0;
        int minR = 255, maxR = 0, minG = 255, maxG = 0, minB = 255, maxB = 0;
        for (int y = 0; y < FBO_SIZE; y += 2) {
            for (int x = 0; x < FBO_SIZE; x += 2) {
                int pixel = image.getPixelRGBA(x, y);
                int r = pixel & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = (pixel >> 16) & 0xFF;
                minR = Math.min(minR, r); maxR = Math.max(maxR, r);
                minG = Math.min(minG, g); maxG = Math.max(maxG, g);
                minB = Math.min(minB, b); maxB = Math.max(maxB, b);
                if (Math.abs(r - bgR) > threshold
                        || Math.abs(g - bgG) > threshold
                        || Math.abs(b - bgB) > threshold) {
                    nonBg++;
                }
            }
        }

        LOG.info("[{}] verifyNonEmpty: nonBg={} R[{},{}] G[{},{}] B[{},{}]",
                name, nonBg, minR, maxR, minG, maxG, minB, maxB);

        if (nonBg < 50) {
            throw new RuntimeException(
                    name + " did not render: only " + nonBg + " non-background samples");
        }
    }
}
