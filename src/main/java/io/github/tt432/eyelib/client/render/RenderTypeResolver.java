package io.github.tt432.eyelib.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

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
