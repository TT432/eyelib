package io.github.tt432.eyelib.util.client;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RenderTypeSerializations {
    public record EntityRenderTypeData(
            ResourceLocation id,
            boolean isSolid,
            Function<ResourceLocation, RenderType> factory
    ) {
    }

    /**
     * TODO: 设置更多的解析
     * @param id id
     * @return EntityRenderTypeData
     */
    public static EntityRenderTypeData getFactory(ResourceLocation id) {
        return switch (id.toString()) {
            case "minecraft:cutout" -> new EntityRenderTypeData(id, false, RenderType::entityCutout);
            case "minecraft:translucent" -> new EntityRenderTypeData(id, false, RenderType::entityTranslucent);
            default -> new EntityRenderTypeData(id, true, RenderType::entitySolid);
        };
    }
}
