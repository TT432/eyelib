package io.github.tt432.eyelib.bridge.material;

import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.material.BrMaterialResolver;
import io.github.tt432.eyelib.material.material.ResolvedBrMaterial;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.material.render.BrRenderState;
import io.github.tt432.eyelib.material.render.BrRenderStateFactory;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 根据资源路径将材质名称解析为对应的 PortRenderPass 工厂。
 * 此桥接版本使用纯数据 Port 类型，不直接依赖 MC RenderType。
 *
 * @author TT432
 */
public final class RenderTypeResolver {
    private RenderTypeResolver() {
    }

    public record EntityRenderTypeData(
            PortResourceLocation id,
            boolean isSolid,
            Function<PortResourceLocation, PortRenderPass> factory
    ) {
    }

    public static EntityRenderTypeData resolve(PortResourceLocation id) {
        return switch (id.toString()) {
            case "minecraft:cutout" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, false));
            case "minecraft:cutout_no_cull" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
            case "minecraft:translucent", "minecraft:particles_blend" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, false));
            case "minecraft:particles_alpha" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
            case "minecraft:particles_add" -> new EntityRenderTypeData(id, false,
                    texture -> BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(particleAdd())));
            default -> new EntityRenderTypeData(id, true,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.SOLID, false));
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
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, false));
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
