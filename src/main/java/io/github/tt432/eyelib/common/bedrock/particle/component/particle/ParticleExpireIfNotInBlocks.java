package io.github.tt432.eyelib.common.bedrock.particle.component.particle;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author DustW
 */
@JsonAdapter(ParticleExpireIfNotInBlocks.class)
@ParticleComponentHolder("minecraft:particle_expire_if_not_in_blocks")
public class ParticleExpireIfNotInBlocks extends ParticleComponent implements JsonDeserializer<ParticleExpireIfNotInBlocks> {
    List<ResourceLocation> blocks;

    @Override
    public ParticleExpireIfNotInBlocks deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ParticleExpireIfNotInBlocks result = new ParticleExpireIfNotInBlocks();
        List<String> deserialize = context.deserialize(json, TypeToken.getParameterized(List.class, String.class).getType());
        result.blocks = deserialize.stream().map(ResourceLocation::new).toList();
        return result;
    }
}
