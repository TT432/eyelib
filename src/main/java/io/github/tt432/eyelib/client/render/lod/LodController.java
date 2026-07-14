package io.github.tt432.eyelib.client.render.lod;

import io.github.tt432.eyelib.client.render.pipeline.FramePlan;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.model.lod.LodPolicy;
import io.github.tt432.eyelib.model.lod.LodRuntimeState;
import net.minecraft.world.entity.Entity;

/**
 * Converts Minecraft camera state into the platform-independent screen-space LOD state.
 */
public final class LodController {
    private static volatile float intensity = 1F;

    private LodController() {
    }

    public static float intensity() {
        return intensity;
    }

    public static void setIntensity(float value) {
        intensity = LodPolicy.normalizeIntensity(value);
    }

    public static void update(LodRuntimeState state, Entity entity, FramePlan plan) {
        var bounds = entity.getBoundingBox();
        double centerX = (bounds.minX + bounds.maxX) * 0.5D;
        double centerY = (bounds.minY + bounds.maxY) * 0.5D;
        double centerZ = (bounds.minZ + bounds.maxZ) * 0.5D;
        double dx = centerX - plan.camX();
        double dy = centerY - plan.camY();
        double dz = centerZ - plan.camZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 1.0E-4D) {
            state.update(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, intensity);
            return;
        }

        var renderSystem = RenderPorts.get().renderSystemPort();
        double fovDegrees = renderSystem.fieldOfViewDegrees();
        double projectionScale = 1D / (2D * distance * Math.tan(Math.toRadians(fovDegrees) * 0.5D));
        float projectedHeight = (float) (entity.getBbHeight() * projectionScale);
        float pixelsPerUnit = (float) (renderSystem.viewportHeight() * projectionScale);
        state.update(projectedHeight, pixelsPerUnit, intensity);
    }
}
