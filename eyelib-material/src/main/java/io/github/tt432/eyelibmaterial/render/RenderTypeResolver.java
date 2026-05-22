package io.github.tt432.eyelibmaterial.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;

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
            case "minecraft:translucent", "minecraft:particles_blend" -> new EntityRenderTypeData(id, false, RenderType::entityTranslucent);
            default -> new EntityRenderTypeData(id, true, RenderType::entitySolid);
        };
    }
}