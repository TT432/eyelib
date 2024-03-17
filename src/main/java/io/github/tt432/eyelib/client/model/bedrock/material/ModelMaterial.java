package io.github.tt432.eyelib.client.model.bedrock.material;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * @author TT432
 */
public record ModelMaterial(
        List<ResourceLocation> textures,
        ResourceLocation renderType
) {
    private static final ResourceLocation solid = new ResourceLocation("solid");

    public static final Codec<ModelMaterial> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ResourceLocation.CODEC.listOf().optionalFieldOf("textures", List.of()).forGetter(o -> o.textures),
            ResourceLocation.CODEC.optionalFieldOf("renderType", solid).forGetter(o -> o.renderType)
    ).apply(ins, ModelMaterial::new));

    public static ModelMaterial parse(JsonObject object) {
        return CODEC.parse(JsonOps.INSTANCE, object).result().orElseThrow();
    }
}
