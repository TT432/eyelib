package io.github.tt432.eyelib.client.render.sections.compat.impl.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Argon4W
 */
public class SodiumCompatImpl {
    public static final ConcurrentHashMap<RenderType, TerrainRenderPass> DYNAMIC_CUTOUT_PASSES = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<RenderType, Material> DYNAMIC_CUTOUT_MATERIALS = new ConcurrentHashMap<>();

    public static final ConcurrentHashMap<RenderType, TerrainRenderPass> DYNAMIC_TRANSLUCENT_PASSES = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<RenderType, Material> DYNAMIC_TRANSLUCENT_MATERIALS = new ConcurrentHashMap<>();

    public static void markCutout(RenderType cutoutRenderType, ResourceLocation textureResourceLocation) {
        SodiumEntityTextureTerrainRenderPass cutoutPass = new SodiumEntityTextureTerrainRenderPass(cutoutRenderType, false, textureResourceLocation);
        Material material = new Material(cutoutPass, AlphaCutoffParameter.ONE_TENTH, true);

        DYNAMIC_CUTOUT_PASSES.putIfAbsent(cutoutRenderType, cutoutPass);
        DYNAMIC_CUTOUT_MATERIALS.putIfAbsent(cutoutRenderType, material);
    }

    public static void markTranslucent(RenderType translucentRenderType, ResourceLocation textureResourceLocation) {
        SodiumEntityTextureTerrainRenderPass translucentPass = new SodiumEntityTextureTerrainRenderPass(translucentRenderType, false, textureResourceLocation);
        Material material = new Material(translucentPass, AlphaCutoffParameter.ONE_TENTH, true);

        DYNAMIC_TRANSLUCENT_PASSES.putIfAbsent(translucentRenderType, translucentPass);
        DYNAMIC_TRANSLUCENT_MATERIALS.putIfAbsent(translucentRenderType, material);
    }
}
