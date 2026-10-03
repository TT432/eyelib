package io.github.tt432.eyelib.bridge.material;
import io.github.tt432.eyelib.bridge.material.adapter.BrRenderTypeFactory;
//? if <1.20.6 {
import io.github.tt432.eyelib.bridge.material.adapter.TextureColorMaterial;
//?}

import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.material.render.BrRenderState;
import io.github.tt432.eyelib.material.render.BrRenderStateFactory;
import io.github.tt432.eyelib.material.render.RenderTypeResolver.EntityRenderTypeData;
import org.jspecify.annotations.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 根据资源路径将材质名称解析为对应的 PortRenderPass 工厂。
 * 此桥接版本使用纯数据 Port 类型，不直接依赖 MC RenderType。
 *
 * @author TT432
 */
public interface RenderTypeResolver {

    Logger LOGGER = LoggerFactory.getLogger("Eyelib/RenderType");
    Set<String> WARNED_UNKNOWN_RENDER_TYPES = ConcurrentHashMap.newKeySet();

    static EntityRenderTypeData resolve(PortResourceLocation id) {
        return switch (id.toString()) {
            //? if <1.20.6 {
            case "eyelib:texture_unlit" -> new EntityRenderTypeData(id, false,
                    tex -> TextureColorMaterial.create(tex, PortRenderPass.Transparency.TRANSLUCENT));
            case "eyelib:texture_unlit_alpha" -> new EntityRenderTypeData(id, false,
                    tex -> TextureColorMaterial.create(tex, PortRenderPass.Transparency.ALPHA_TEST));
            case "eyelib:texture_unlit_add" -> new EntityRenderTypeData(id, false,
                    tex -> TextureColorMaterial.create(tex, PortRenderPass.Transparency.ADDITIVE));
            case "eyelib:texture_unlit_opaque" -> new EntityRenderTypeData(id, true,
                    tex -> TextureColorMaterial.create(tex, PortRenderPass.Transparency.SOLID));
            //?}
            case "minecraft:cutout" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, false));
            case "minecraft:cutout_no_cull" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
            case "minecraft:translucent", "minecraft:particles_blend" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, true));
            case "minecraft:particles_alpha" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
            case "minecraft:particles_add" -> new EntityRenderTypeData(id, false,
                    texture -> BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(particleAdd())));
            default -> {
                EntityRenderTypeData vanilla = resolveVanilla(id);
                if (vanilla != null) {
                    yield vanilla;
                }
                if (WARNED_UNKNOWN_RENDER_TYPES.add(id.toString())) {
                    LOGGER.warn("Unknown material '{}' - no matching material definition or render type found. " +
                            "Falling back to SOLID; add the material to eyelib/materials/ for correct rendering.", id);
                }
                yield new EntityRenderTypeData(id, true,
                        tex -> PortRenderPass.of(PortRenderPass.Transparency.SOLID, false));
            }
        };
    }

    /**
     * 当材质名未在 eyelib 材质系统中命中时，按名称匹配原版 (MC JE) RenderType 语义。
     * <p>
     * 命中原版实体/方块层 RenderType 名称时返回等价 {@link PortRenderPass}（透明度 + cull），
     * 由 {@link io.github.tt432.eyelib.bridge.material.adapter.RenderPassAdapter} 物化为对应版本的 MC RenderType。
     * <p>
     * cull 按版本对齐原版：原版 {@code entityCutout} 在 1.20.1/1.21.1 为剔除，
     * 在 26.1.2 渲染重写后翻转为不剔除（名称 {@code entity_cutout} 不再剔除）。
     * 未命中返回 {@code null}，调用方回退到 SOLID + 警告。
     */
    private static @Nullable EntityRenderTypeData resolveVanilla(PortResourceLocation id) {
        return switch (id.path()) {
            // --- 不透明 ---
            case "entity_solid", "solid" -> new EntityRenderTypeData(id, true,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.SOLID, false));
            // --- alpha test ---
            case "entity_cutout" -> {
                //? if <26.1 {
                yield new EntityRenderTypeData(id, false,
                        tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, false));
                //?} else {
                yield new EntityRenderTypeData(id, false,
                        tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
                //?}
            }
            case "entity_cutout_no_cull", "entity_cutout_no_cull_z_offset" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
            case "cutout", "cutout_mipped" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, false));
            // --- 半透明 ---
            case "entity_translucent" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, true));
            case "entity_translucent_cull" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, false));
            case "translucent" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, true));
            // --- 自发光（eyes 近似为 emissive，优于 SOLID 回退）---
            case "entity_translucent_emissive", "eyes" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT_EMISSIVE, false));
            default -> null;
        };
    }

    public static PortRenderPass resolve(PortResourceLocation texture, BrMaterialEntry entry, Map<String, BrMaterialEntry> materials) {
        try {
            ResolvedBrMaterial material = BrMaterialResolver.resolve(entry, materials);
            return BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(material));
        } catch (IllegalStateException e) {
            // 材质继承链存在循环引用，fallback 到仅检查自身状态
            if (entry.hasBlending(Map.of())) return PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, false);
            if (entry.isAlphatest(Map.of())) return PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, false);
            return PortRenderPass.of(PortRenderPass.Transparency.SOLID, false);
        }
    }

    public static PortRenderPass resolve(PortResourceLocation texture, ResolvedBrMaterial material) {
        Map<PortResourceLocation, PortRenderPass> byTexture = RESOLVED_PASS_CACHE.get(material);
        if (byTexture != null) {
            PortRenderPass cached = byTexture.get(texture);
            if (cached != null) {
                return cached;
            }
        }
        PortRenderPass created = BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(material));
        RESOLVED_PASS_CACHE.computeIfAbsent(material, m -> new ConcurrentHashMap<>()).put(texture, created);
        return created;
    }

    /**
     * (ResolvedBrMaterial identity, texture) → PortRenderPass 缓存。
     * ResolvedBrMaterial 由 BrMaterialResolver 全局缓存（同一材质代际内实例唯一），
     * 渲染热路径每帧每组件的 Key/BridgeRenderPass 分配由此消除。
     */
    Map<ResolvedBrMaterial, Map<PortResourceLocation, PortRenderPass>> RESOLVED_PASS_CACHE =
            java.util.Collections.synchronizedMap(new java.util.IdentityHashMap<>());

    /** 材质代际更换（addon 重载）后清空派生缓存：pass 缓存与 BrRenderState 记忆化。 */
    static void clearDerivedCaches() {
        RESOLVED_PASS_CACHE.clear();
        BrRenderStateFactory.clearCache();
    }

    public static boolean isSolid(ResolvedBrMaterial material) {
        return BrRenderStateFactory.from(material).isSolid();
    }

    public static boolean isSolid(BrMaterialEntry entry, Map<String, BrMaterialEntry> materials) {
        try {
            ResolvedBrMaterial material = BrMaterialResolver.resolve(entry, materials);
            return BrRenderStateFactory.from(material).isSolid();
        } catch (IllegalStateException e) {
            return !entry.hasBlending(Map.of()) && !entry.isAlphatest(Map.of());
        }
    }

    public static boolean isAlphaTest(BrMaterialEntry entry, Map<String, BrMaterialEntry> materials) {
        try {
            ResolvedBrMaterial material = BrMaterialResolver.resolve(entry, materials);
            return BrRenderStateFactory.from(material).transparency() == BrRenderState.Transparency.ALPHA_TEST;
        } catch (IllegalStateException e) {
            return entry.isAlphatest(Map.of());
        }
    }
    /**
     * 粒子材质解析（ADR-0033）：透明度/cull 映射与 domain 版一致，但统一构造带光照轴的
     * {@link BrRenderState} 走 {@link BrRenderTypeFactory}——粒子光照无方向项，
     * vanilla entity shader 的方向光对 lit/unlit 粒子都是误用（视角相关明暗）。
     *
     * @param lit 是否存在 {@code minecraft:particle_appearance_lighting} 组件：
     *            true → AMBIENT（仅环境光 tint），false → NONE（全亮）
     */
    public static EntityRenderTypeData resolveParticle(String materialName, boolean lit) {
        PortResourceLocation id = materialName.contains(":")
                ? PortResourceLocation.parse(materialName)
                : PortResourceLocation.of("minecraft", materialName);
        BrRenderState.LightingModel lighting = lit
                ? BrRenderState.LightingModel.AMBIENT
                : BrRenderState.LightingModel.NONE;
        return switch (id.path()) {
            case "particles_opaque", "particles_base" -> new EntityRenderTypeData(id, true,
                    tex -> BrRenderTypeFactory.create(tex, particleState(
                            BrRenderState.SurfaceClass.OPAQUE, BrRenderState.Transparency.NONE, true, lighting)));
            case "particles_alpha" -> new EntityRenderTypeData(id, false,
                    tex -> BrRenderTypeFactory.create(tex, particleState(
                            BrRenderState.SurfaceClass.CUTOUT, BrRenderState.Transparency.ALPHA_TEST, false, lighting)));
            case "particles_blend" -> new EntityRenderTypeData(id, false,
                    tex -> BrRenderTypeFactory.create(tex, particleState(
                            BrRenderState.SurfaceClass.TRANSLUCENT, BrRenderState.Transparency.BLEND, false, lighting)));
            case "particles_add" -> new EntityRenderTypeData(id, false,
                    tex -> BrRenderTypeFactory.create(tex,
                            BrRenderStateFactory.from(particleAdd()).withLighting(lighting)));
            // 非内建粒子材质：按实体材质语义解析（方向光），与原 fallback 行为一致
            default -> resolve(id);
        };
    }

    /** 内建粒子材质的渲染状态：默认深度/写掩码/无模板，lightmap/overlay 直通（同原 vanilla RenderType 组合）。 */
    private static BrRenderState particleState(
            BrRenderState.SurfaceClass surfaceClass,
            BrRenderState.Transparency transparency,
            boolean cull,
            BrRenderState.LightingModel lighting
    ) {
        return new BrRenderState(
                surfaceClass,
                cull,
                transparency,
                new BrRenderState.Depth(true, Optional.empty()),
                new BrRenderState.WriteMask(true, true),
                Optional.empty(),
                Optional.empty(),
                true,
                true,
                Set.of(),
                false,
                false,
                lighting
        );
    }

    private static ResolvedBrMaterial particleAdd() {
        return new ResolvedBrMaterial(
                "particles_add",
                java.util.List.of("particles_add"),
                Optional.empty(),
                Optional.empty(),
                java.util.Set.of(),
                java.util.Set.of(GLStates.Blending, GLStates.DisableCulling),
                java.util.List.of(),
                Optional.empty(),
                new ResolvedBrMaterial.BlendState(
                        io.github.tt432.eyelib.material.gl.BlendFactor.SourceAlpha,
                        io.github.tt432.eyelib.material.gl.BlendFactor.One,
                        io.github.tt432.eyelib.material.gl.BlendFactor.One,
                        io.github.tt432.eyelib.material.gl.BlendFactor.OneMinusSrcAlpha
                ),
                ResolvedBrMaterial.StencilState.DEFAULT,
                java.util.List.of()
        );
    }
}

