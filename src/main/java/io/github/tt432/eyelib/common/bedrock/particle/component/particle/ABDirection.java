package io.github.tt432.eyelib.common.bedrock.particle.component.particle;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.util.Value3;
import io.github.tt432.eyelib.util.json.JsonUtils;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ABDirection.class)
public class ABDirection implements JsonDeserializer<ABDirection> {
    Mode mode;

    /**
     * only used in "derive_from_velocity" mode.
     * The direction is set if the speed of the particle is above the threshold.
     * The default is 0.01
     */
    @SerializedName("min_speed_threshold")
    double minSpeedThreshold;

    /**
     * only used in "custom_direction" mode. Specifies the direction vector
     */
    @SerializedName("custom_direction")
    Value3 customDirection;

    // TODO: UV 序列化

    @Override
    public ABDirection deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ABDirection result = new ABDirection();
        JsonObject object = json.getAsJsonObject();
        result.mode = context.deserialize(object, Mode.class);

        if (mode == Mode.DERIVE_FROM_VELOCITY) {
            result.minSpeedThreshold = JsonUtils.parseOrDefault(context, object, "", double.class, .01);
        } else if (mode == Mode.CUSTOM) {
            result.customDirection = context.deserialize(object.get("custom_direction"), Value3.class);
        } else {
            throw new JsonParseException("can't parse direction : " + json);
        }

        return result;
    }

    public enum Mode {
        /**
         * The direction matches the direction of the velocity.
         */
        @SerializedName("derive_from_velocity")
        DERIVE_FROM_VELOCITY,
        /**
         * The direction is specified in the json definition using a vector of floats or molang expressions.
         */
        @SerializedName("custom")
        CUSTOM
    }
}
