package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.math.Constant;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ESSphere.class)
@ParticleComponentHolder("minecraft:emitter_shape_sphere")
public class ESSphere extends EmitterShapeComponent implements JsonDeserializer<ESSphere> {
    /**
     * sphere radius
     * evaluated once per particle emitted
     */
    MolangValue radius;

    @Override
    public ESSphere deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESSphere result = new ESSphere();
        JsonObject object = json.getAsJsonObject();
        processBase(result, object, context);
        result.radius = JsonUtils.parseOrDefault(context, object, "radius", MolangValue.class, new Constant(1));
        return result;
    }
}
