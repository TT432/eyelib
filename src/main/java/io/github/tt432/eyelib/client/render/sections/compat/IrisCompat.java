package io.github.tt432.eyelib.client.render.sections.compat;

import io.github.tt432.eyelib.client.render.sections.cache.QuadListBakingVertexConsumer;
import io.github.tt432.eyelib.client.render.sections.compat.impl.IrisCompatImpl;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.fml.ModList;

import java.util.HashMap;

public final class IrisCompat {
    private static final boolean INSTALLED = ModList.get().isLoaded("iris");

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isRenderingShadow() {
        return isInstalled() && IrisCompatImpl.isRenderingShadow();
    }

    public static RenderType unwrapRenderType(RenderType renderType) {
        return isInstalled() ? IrisCompatImpl.unwrap(renderType) : renderType;
    }

    public static MultiBufferSource getEntityBakingBufferSource(HashMap<RenderType, QuadListBakingVertexConsumer> builder) {
        return renderType -> builder.computeIfAbsent(unwrapRenderType(renderType), QuadListBakingVertexConsumer::new);
    }
}
