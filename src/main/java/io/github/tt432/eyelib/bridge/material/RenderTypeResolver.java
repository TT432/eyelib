package io.github.tt432.eyelib.bridge.material;
import io.github.tt432.eyelib.bridge.material.adapter.BrRenderTypeFactory;

import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.material.render.BrRenderState;
import io.github.tt432.eyelib.material.render.BrRenderStateFactory;
import io.github.tt432.eyelib.material.render.RenderTypeResolver.EntityRenderTypeData;
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
    private static EntityRenderTypeData resolveVanilla(PortResourceLocation id) {
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
        return BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(material));
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

    public static EntityRenderTypeData resolveParticle(String materialName) {
        PortResourceLocation id = materialName.contains(":")
                ? PortResourceLocation.parse(materialName)
                : PortResourceLocation.of("minecraft", materialName);
        return switch (id.path()) {
            case "particles_opaque", "particles_base" -> new EntityRenderTypeData(id, true,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.SOLID, false));
            case "particles_alpha" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
            case "particles_blend" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, true));
            case "particles_add" -> new EntityRenderTypeData(id, false,
                    texture -> BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(particleAdd())));
            default -> resolve(id);
        };
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

