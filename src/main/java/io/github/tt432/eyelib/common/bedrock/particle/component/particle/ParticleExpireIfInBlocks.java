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
@JsonAdapter(ParticleExpireIfInBlocks.class)
@ParticleComponentHolder("minecraft:particle_expire_if_in_blocks")
public class ParticleExpireIfInBlocks extends ParticleComponent implements JsonDeserializer<ParticleExpireIfInBlocks> {
    List<ResourceLocation> blocks;

    @Override
    public ParticleExpireIfInBlocks deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ParticleExpireIfInBlocks result = new ParticleExpireIfInBlocks();
        List<String> deserialize =  context.deserialize(json, TypeToken.getParameterized(List.class, String.class).getType());
        result.blocks = deserialize.stream().map(ResourceLocation::new).toList();
        return result;
    }
}
