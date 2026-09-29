package io.github.tt432.eyelib.bridge.particle.adapter;

//? if <26.1 {
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
//?} else {
import net.minecraft.client.renderer.rendertype.RenderType;
//?}
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * wintersky {@code materialType × 纹理} → MC RenderType 直映射（ADR-0035 D2）。
 *
 * <p>wintersky 材质语义：alpha=NormalBlending+discard（FrontSide）、opaque=a=1（FrontSide）、
 * blend=NormalBlending+depthWrite off（DoubleSide）、add=AdditiveBlending+depthWrite off（DoubleSide）。
 * 光照恒 FULL_BRIGHT（wintersky 无光照概念）。
 *
 * <p>全部使用自定义 RenderType（非 vanilla 基座）。实机实证（2026-09-29 debug quad 探针）：
 * vanilla entitySolid/entityCutout/entityTranslucentEmissive 基座在 AFTER_ENTITIES 阶段
 * 写入的顶点不被绘制，自定义 RenderType 与生产 {@code BrRenderTypeFactory} 同路径正常绘制。
 */
public final class WinterskyRenderTypes {
    private WinterskyRenderTypes() {
    }

    //? if <26.1 {
    /** wintersky NormalBlending + depthWrite off（blend 材质语义）。 */
    private static final RenderStateShard.TransparencyStateShard WINTERSKY_TRANSLUCENT =
            new RenderStateShard.TransparencyStateShard("wintersky_translucent", () -> {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }, () -> {
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            });

    /** wintersky AdditiveBlending（SRC_ALPHA, ONE）+ depthWrite off（add 材质语义）。 */
    private static final RenderStateShard.TransparencyStateShard WINTERSKY_ADDITIVE =
            new RenderStateShard.TransparencyStateShard("wintersky_additive", () -> {
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(
                        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                        com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                        com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            }, () -> {
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            });

    private static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard("wintersky_no_transparency", () -> {}, () -> {});

    private static RenderType custom(String name, ResourceLocation texture,
                                     Supplier<ShaderInstance> shader,
                                     RenderStateShard.TransparencyStateShard transparency,
                                     boolean cull, boolean writeDepth) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(new RenderStateShard.ShaderStateShard(shader))
                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                .setTransparencyState(transparency)
                .setCullState(new RenderStateShard.CullStateShard(cull))
                .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                .setOverlayState(new RenderStateShard.OverlayStateShard(true))
                .setDepthTestState(new RenderStateShard.DepthTestStateShard("<=", 515))
                .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, writeDepth))
                .createCompositeState(false);
        return RenderType.create(name, DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, true, false, state);
    }
    //?}

    private record Key(String material, ResourceLocation texture) {
    }

    private static final Map<Key, Object> CACHE = new HashMap<>();

    /**
     * 按 wintersky 材质名取 RenderType（以 Object 存放，消费点按版本 {@code //?} 转型，
     * 同 {@link BedrockParticleRenderer} 的 renderType 处理）。
     */
    public static Object get(String material, ResourceLocation texture) {
        return CACHE.computeIfAbsent(new Key(material, texture),
                key -> create(key.material(), key.texture()));
    }

    private static Object create(String material, ResourceLocation texture) {
        //? if <26.1 {
        return switch (material) {
            // opaque：a=1，FrontSide，depthWrite on
            case "particles_opaque" -> custom("eyelib_wintersky_opaque", texture,
                    GameRenderer::getRendertypeEntitySolidShader, NO_TRANSPARENCY, true, true);
            // blend：NormalBlending + depthWrite off，DoubleSide
            case "particles_blend" -> custom("eyelib_wintersky_blend", texture,
                    GameRenderer::getRendertypeEntityTranslucentShader, WINTERSKY_TRANSLUCENT, false, false);
            // add：AdditiveBlending + depthWrite off，DoubleSide
            case "particles_add" -> custom("eyelib_wintersky_add", texture,
                    GameRenderer::getRendertypeEntityTranslucentShader, WINTERSKY_ADDITIVE, false, false);
            // alpha（默认）：discard 近似 cutout，FrontSide，depthWrite on
            default -> custom("eyelib_wintersky_alpha", texture,
                    GameRenderer::getRendertypeEntityCutoutShader, NO_TRANSPARENCY, true, true);
        };
        //?} else {
        // 26.1 PSO 路径（ADR-0035 D2/§3.3）：P3 多版本期补 RenderSetup 直映射，
        // 现阶段用 vanilla 基座近似（add 退化为 translucent emissive）。
        return switch (material) {
            case "particles_opaque" -> RenderType.entitySolid(texture);
            case "particles_blend", "particles_add" -> RenderType.entityTranslucentEmissive(texture);
            default -> RenderType.entityCutout(texture);
        };
        //?}
    }

    /** 资源重载/切世界时清空缓存（RenderType 与纹理绑定，条目廉价可重建）。 */
    public static void clearCache() {
        CACHE.clear();
    }
}
