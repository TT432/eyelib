package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.Value3;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.Constant;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ESDisc.class)
@ParticleComponentHolder("minecraft:emitter_shape_disc")
public class ESDisc extends EmitterShapeComponent implements JsonDeserializer<ESDisc> {
    /**
     * specifies the normal of the disc plane, the disc will be perpendicular to this direction
     * <p>
     * defaults to [ 0, 1, 0 ]
     */
    @SerializedName("plane_normal")
    Value3 planeNormal;

    /**
     * disc radius
     * evaluated once per particle emitted
     * default:1
     */
    MolangValue radius;

    @Override
    public ESDisc deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ESDisc result = new ESDisc();
        JsonObject object = json.getAsJsonObject();
        processBase(result, object, context);

        if (object.has("plane_normal")) {
            JsonElement normal = object.get("plane_normal");

            if (normal.isJsonPrimitive()) {
                String axis = normal.getAsString();

                result.planeNormal = switch (axis) {
                    case "x" -> new Value3(new Constant(1), new Constant(0), new Constant(0));
                    case "y" -> new Value3(new Constant(0), new Constant(1), new Constant(0));
                    case "z" -> new Value3(new Constant(0), new Constant(0), new Constant(1));
                    default -> throw new JsonParseException("plane_normal format error : " + axis);
                };
            } else if (normal.isJsonArray()) {
                result.planeNormal = context.deserialize(normal, Value3.class);
            }
        } else {
            result.planeNormal = new Value3(new Constant(0), new Constant(1), new Constant(0));
        }

        result.radius = JsonUtils.parseOrDefault(context, object, "radius", MolangValue.class, new Constant(1));

        return result;
    }
}
