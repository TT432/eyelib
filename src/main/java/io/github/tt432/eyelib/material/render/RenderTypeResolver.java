package io.github.tt432.eyelib.material.render;

import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import java.util.Map;
import java.util.function.Function;

/**
 * 基于 Port 类型的 RenderType 解析器。
 * 此版本不依赖 MC 类型，供 domain 及下游模块使用。
 * MC RenderType 的生成由 eyelib-bridge 中的 RenderTypeResolver 负责。
 *
 * @author TT432
 */
public final class RenderTypeResolver {

    private RenderTypeResolver() {}

    public record EntityRenderTypeData(
            PortResourceLocation id,
            boolean isSolid,
            Function<PortResourceLocation, PortRenderPass> factory
    ) {}

    /**
     * 根据 RenderType 标识返回渲染数据。
     */
    public static EntityRenderTypeData resolve(PortResourceLocation id) {
        return switch (id.path()) {
            case "solid" -> new EntityRenderTypeData(id, true,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.SOLID, false));
            case "cutout" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, false));
            case "cutout_no_cull" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true));
            case "translucent" -> new EntityRenderTypeData(id, false,
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, true));
            default -> throw new IllegalArgumentException("未知 RenderType: " + id);
        };
    }

    /**
     * 根据材质和纹理解析 PortRenderPass。
     * 委托给 BrMaterialEntry 自有的语义计算。
     */
    public static PortRenderPass resolve(
            PortResourceLocation texture,
            BrMaterialEntry entry,
            Map<String, BrMaterialEntry> materials
    ) {
        return entry.getRenderType(texture, materials);
    }

    /**
     * 根据粒子材质名称返回渲染数据。
     */
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
                    tex -> PortRenderPass.of(PortRenderPass.Transparency.ADDITIVE, true));
            default -> resolve(id);
        };
    }
}
