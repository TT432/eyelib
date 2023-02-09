package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;

import java.lang.reflect.Type;

/**
 * offset 为跟随玩家
 *
 * @author DustW
 */
@JsonAdapter(ESEntityAABB.class)
@ParticleComponentHolder("minecraft:emitter_shape_entity_aabb")
public class ESEntityAABB extends EmitterShapeComponent implements JsonDeserializer<ESEntityAABB> {
    @Override
    public ESEntityAABB deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESEntityAABB result = new ESEntityAABB();
        processBase(result, json.getAsJsonObject(), context);
        return result;
    }
}
