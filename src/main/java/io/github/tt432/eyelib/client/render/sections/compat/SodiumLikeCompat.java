package io.github.tt432.eyelib.client.render.sections.compat;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.tt432.eyelib.client.render.sections.compat.impl.embeddium.EmbeddiumCompatImpl;
import io.github.tt432.eyelib.client.render.sections.compat.impl.sodium.SodiumCompatImpl;
import io.github.tt432.eyelib.client.render.sections.events.SectionGeometryRenderTypeEvents;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class SodiumLikeCompat {
    private static final boolean SODIUM_INSTALLED = ModList.get().isLoaded("sodium");
    private static final boolean EMBEDDIUM_INSTALLED = ModList.get().isLoaded("embeddium");

    public static boolean isInstalled() {
        return SODIUM_INSTALLED || EMBEDDIUM_INSTALLED;
    }

    public static boolean isSodiumInstalled() {
        return SODIUM_INSTALLED;
    }

    public static boolean isEmbeddiumInstalled() {
        return EMBEDDIUM_INSTALLED;
    }

    public static RenderLevelStageEvent.Stage getCutoutRenderStage() {
        return isInstalled() ? RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS : RenderLevelStageEvent.Stage.AFTER_PARTICLES;
    }

    public static RenderType addSodiumCutoutPass(ResourceLocation resourceLocation, RenderType cutoutRenderType) {
        return Util.make(cutoutRenderType, renderType -> {
            if (isSodiumInstalled()) {
                SodiumCompatImpl.markCutout(renderType, resourceLocation);
            } else if (isEmbeddiumInstalled()) {
                EmbeddiumCompatImpl.markCutout(renderType, resourceLocation);
            }
        });
    }

    public static RenderType addSodiumTranslucentPass(ResourceLocation resourceLocation, RenderType translucentRenderType) {
        return Util.make(translucentRenderType, renderType -> {
            if (isSodiumInstalled()) {
                SodiumCompatImpl.markTranslucent(renderType, resourceLocation);
            } else if (isEmbeddiumInstalled()) {
                EmbeddiumCompatImpl.markTranslucent(renderType, resourceLocation);
            }
        });
    }

    public static RenderType createCutoutRenderType(ResourceLocation textureResourceLocation) {
        if (isInstalled()) {
            return RenderType.create("eyelib_sodium_like_dummy_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false, RenderType.CompositeState.builder()
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true));
        }

        return SectionGeometryRenderTypeEvents.getEntityCutoutNoCull(textureResourceLocation);
    }

    public static RenderType createTranslucentRenderType(ResourceLocation textureResourceLocation) {
        if (isInstalled()) {
            return RenderType.create("eyelib_sodium_like_dummy_translucent", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true, RenderType.CompositeState.builder()
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true));
        }

        return SectionGeometryRenderTypeEvents.getEntityTranslucent(textureResourceLocation);
    }
}
