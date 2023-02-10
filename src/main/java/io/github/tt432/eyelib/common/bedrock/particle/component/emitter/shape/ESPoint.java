package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;

import java.lang.reflect.Type;

/**
 * surfaceOnly 无效
 *
 * @author DustW
 */
@JsonAdapter(ESPoint.class)
@ParticleComponentHolder("minecraft:emitter_shape_point")
public class ESPoint extends EmitterShapeComponent implements JsonDeserializer<ESPoint> {
    @Override
    public ESPoint deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESPoint result = new ESPoint();
        processBase(result, json.getAsJsonObject(), context);
        return result;
    }
}
