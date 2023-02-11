package io.github.tt432.eyelib.common.bedrock.particle.component.emitter.shape;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.molang.util.Value3;

/**
 * "direction": "inwards" // particle direction towards center of disc
 * "direction": "outwards" // particle direction away from center of disc
 *
 * @author DustW
 */
@JsonAdapter(Direction.class)
public class Direction implements JsonDeserializer<Direction> {
    Value3 value;
    Type type;

    public static Direction defaultValue() {
        Direction direction = new Direction();
        direction.type = Type.OUTWARDS;
        return direction;
    }

    @Override
    public Direction deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Direction direction = new Direction();

        if (json.isJsonPrimitive()) {
            if (json.getAsString().equals("inwards")) {
                direction.type = Type.INWARDS;
            } else if (json.getAsString().equals("outwards")) {
                direction.type = Type.OUTWARDS;
            } else {
                throw new JsonParseException("can't parse direction for value : " + json);
            }
        } else if (json.isJsonArray()) {
            direction.type = Type.CUSTOM;
            direction.value = context.deserialize(json, Value3.class);
        }

        return direction;
    }

    public enum Type {
        INWARDS,
        OUTWARDS,
        CUSTOM
    }
}
