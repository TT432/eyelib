package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;

import java.lang.reflect.Type;

/**
 * 仅表面选项无效
 *
 * @author DustW
 */
@JsonAdapter(ESCustom.class)
@ParticleComponentHolder("minecraft:emitter_shape_custom")
public class ESCustom extends EmitterShapeComponent implements JsonDeserializer<ESCustom> {
    @Override
    public ESCustom deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESCustom custom = new ESCustom();
        processBase(custom, json.getAsJsonObject(), context);
        return custom;
    }
}
