package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.molang.util.Value3;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.molang.math.Constant;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ESBox.class)
@ParticleComponentHolder("minecraft:emitter_shape_box")
public class ESBox extends EmitterShapeComponent implements JsonDeserializer<ESBox> {
    /**
     * box dimensions
     * these are the half dimensions, the box is formed centered on the emitter
     * with the box extending in the 3 principal x/y/z axes by these values
     */
    @SerializedName("half_dimensions")
    Value3 halfDimensions;

    @Override
    public ESBox deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESBox result = new ESBox();
        JsonObject object = json.getAsJsonObject();
        processBase(result, object, context);

        result.halfDimensions = JsonUtils.parseOrDefault(context, object, "half_dimensions", Value3.class,
                new Value3(new Constant(0), new Constant(0), new Constant(0)));

        return result;
    }
}
