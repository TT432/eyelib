package io.github.tt432.eyelibmaterial.render;

import io.github.tt432.eyelibmaterial.gl.GLStates;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import io.github.tt432.eyelibmaterial.material.BrMaterialResolver;
import io.github.tt432.eyelibmaterial.material.ResolvedBrMaterial;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 根据资源路径将材质名称解析为对应的RenderType工厂。
 *
 * @author TT432
 */
@NullMarked
public final class RenderTypeResolver {
    private RenderTypeResolver() {
    }

    public record EntityRenderTypeData(
            ResourceLocation id,
            boolean isSolid,
            Function<ResourceLocation, RenderType> factory
    ) {
    }

    public static EntityRenderTypeData resolve(ResourceLocation id) {
        return switch (id.toString()) {
            case "minecraft:cutout" -> new EntityRenderTypeData(id, false, RenderType::entityCutout);
            case "minecraft:cutout_no_cull" -> new EntityRenderTypeData(id, false, RenderType::entityCutoutNoCull);
            case "minecraft:translucent", "minecraft:particles_blend" -> new EntityRenderTypeData(id, false, RenderType::entityTranslucent);
            case "minecraft:particles_alpha" -> new EntityRenderTypeData(id, false, RenderType::entityCutoutNoCull);
            case "minecraft:particles_add" -> new EntityRenderTypeData(id, false,
                    texture -> BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(particleAdd())));
            default -> new EntityRenderTypeData(id, true, RenderType::entitySolid);
        };
    }

    public static RenderType resolve(ResourceLocation texture, BrMaterialEntry entry, Map<String, BrMaterialEntry> materials) {
        try {
            ResolvedBrMaterial material = BrMaterialResolver.resolve(entry, materials);
            return BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(material));
        } catch (IllegalStateException e) {
            // 材质继承链存在循环引用，fallback 到仅检查自身状态
            if (entry.hasBlending(Map.of())) return RenderType.entityTranslucent(texture);
            if (entry.isAlphatest(Map.of())) return RenderType.entityCutoutNoCull(texture);
            return RenderType.entitySolid(texture);
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
        ResourceLocation id = materialName.contains(":") ? new ResourceLocation(materialName) : new ResourceLocation("minecraft", materialName);
        return switch (id.getPath()) {
            case "particles_opaque", "particles_base" -> new EntityRenderTypeData(id, true, RenderType::entitySolid);
            case "particles_alpha" -> new EntityRenderTypeData(id, false, RenderType::entityCutoutNoCull);
            case "particles_blend" -> new EntityRenderTypeData(id, false, RenderType::entityTranslucent);
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
                        io.github.tt432.eyelibmaterial.gl.BlendFactor.SourceAlpha,
                        io.github.tt432.eyelibmaterial.gl.BlendFactor.One,
                        io.github.tt432.eyelibmaterial.gl.BlendFactor.One,
                        io.github.tt432.eyelibmaterial.gl.BlendFactor.OneMinusSrcAlpha
                ),
                ResolvedBrMaterial.StencilState.DEFAULT,
                java.util.List.of()
        );
    }
}
