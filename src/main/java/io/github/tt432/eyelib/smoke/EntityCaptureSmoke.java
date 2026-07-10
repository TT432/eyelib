package io.github.tt432.eyelib.smoke;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.tt432.clientsmoke.runtime.ClientSmokeVisualHooks;
import io.github.tt432.clientsmoke.runtime.EntitySceneRenderer;
import io.github.tt432.clientsmokeannotation.ClientSmoke;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smoke test: renders four vanilla entities (spider, slime, sheep, creeper)
 * into a dedicated FBO in a 2×2 grid using {@link EntitySceneRenderer}, then
 * verifies via pixel analysis that each entity actually rendered.
 *
 * <p>This test exercises the full entity-to-FBO pipeline: entity creation,
 * orthographic projection setup, lighting, entity render dispatch, buffer
 * flush, and framebuffer capture.</p>
 *
 * @author TT432
 */
@ClientSmoke(
        description = "将特定实体渲染到 FBO 并截图验证（蜘蛛、史莱姆、羊、苦力怕）",
        priority = 5
)
public class EntityCaptureSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityCaptureSmoke.class);

    private static final int FBO_W = 512;
    private static final int FBO_H = 512;
    private static final int BG_ARGB = 0xFF1A1A2E;
    private static final int CELL = FBO_W / 2;

    public EntityCaptureSmoke() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            throw new AssertionError("Level not available");
        }

        final float scale = 100;

        ClientSmokeVisualHooks.setScene(FBO_W, FBO_H, (m, rt) -> {
            Entity spider = createAndInit(mc, EntityType.SPIDER);
            Entity slime = createAndInit(mc, EntityType.SLIME);
            Entity sheep = createAndInit(mc, EntityType.SHEEP);
            Entity creeper = createAndInit(mc, EntityType.CREEPER);

            EntitySceneRenderer.beginScene(m, rt, BG_ARGB);
            EntitySceneRenderer.renderEntityAt(m, spider,  cellCenterX(0), CELL / 2F,  scale, 225);
            EntitySceneRenderer.renderEntityAt(m, slime,   cellCenterX(1), CELL / 2F,  scale, -45);
            EntitySceneRenderer.renderEntityAt(m, sheep,   cellCenterX(0), CELL * 1.5F, scale, 30);
            EntitySceneRenderer.renderEntityAt(m, creeper, cellCenterX(1), CELL * 1.5F, scale, 0);
            EntitySceneRenderer.endScene(m);
        }, image -> {
            verifyEntityRendered(image);
        });
    }

    private static float cellCenterX(int col) {
        return col * CELL + CELL / 2.0F;
    }

    private static Entity createAndInit(Minecraft mc, EntityType<?> type) {
        Entity entity = type.create(mc.level);
        if (entity == null) {
            throw new AssertionError("Failed to create entity: " + type);
        }
        entity.setPos(0, 0, 0);
        entity.tickCount = 10;
        return entity;
    }

    /**
     * Verifies that the captured image contains non-background pixels in each
     * of the four quadrants, proving that all four entities rendered.
     *
     * <p>Note: {@code NativeImage.getPixelRGBA} returns RGBA format where
     * R = bits 0-7, G = bits 8-15, B = bits 16-23. The background is cleared
     * via {@code RenderSystem.clearColor} with the same channels.</p>
     */
    private static void verifyEntityRendered(NativeImage image) {
        int bgR = (BG_ARGB >> 16) & 0xFF;
        int bgG = (BG_ARGB >> 8) & 0xFF;
        int bgB = BG_ARGB & 0xFF;

        String[] names = {"Spider", "Slime", "Sheep", "Creeper"};
        int[][] origins = {
                {0, 0},         // top-left
                {FBO_W / 2, 0}, // top-right
                {0, FBO_H / 2}, // bottom-left
                {FBO_W / 2, FBO_H / 2} // bottom-right
        };

        for (int q = 0; q < 4; q++) {
            int ox = origins[q][0];
            int oy = origins[q][1];
            int nonBg = 0;
            int threshold = 15;

            for (int y = oy; y < oy + FBO_H / 2; y += 3) {
                for (int x = ox; x < ox + FBO_W / 2; x += 3) {
                    int pixel = image.getPixelRGBA(x, y);
                    int r = pixel & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = (pixel >> 16) & 0xFF;
                    if (Math.abs(r - bgR) > threshold
                            || Math.abs(g - bgG) > threshold
                            || Math.abs(b - bgB) > threshold) {
                        nonBg++;
                    }
                }
            }

            LOGGER.info("[EntityCaptureSmoke] {} quadrant: {} non-background samples", names[q], nonBg);
            if (nonBg < 30) {
                throw new RuntimeException(
                        names[q] + " did not render: only " + nonBg + " non-background pixels found");
            }
        }

        LOGGER.info("[EntityCaptureSmoke] All four entities rendered successfully");
    }
}
