package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.util.json.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;

@JsonAdapter(Locator.Serializer.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Locator {
    /**
     * Discard scale inherited from parent bone.
     */
    @SerializedName("ignore_inherited_scale")
    private boolean ignoreInheritedScale;
    /**
     * Position of the locator in model space.
     */
    private double[] offset;
    /**
     * Rotation of the locator in model space.
     */
    private double[] rotation;

    public Locator copy() {
        return new Locator(
                ignoreInheritedScale,
                new double[]{offset[0], offset[1], offset[2]},
                new double[]{rotation[0], rotation[1], rotation[2]}
        );
    }

    protected static class Serializer implements JsonDeserializer<Locator> {
        @Override
        public Locator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonArray()) {
                double[] doubleArrayValue = context.deserialize(json, double[].class);
                return new Locator(false, doubleArrayValue, new double[]{0, 0, 0});
            } else if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                return new Locator(
                        JsonUtils.parseOrDefault(context, object, "ignore_inherited_scale", boolean.class, false),
                        JsonUtils.parseOrDefault(context, object, "offset", boolean[].class, new double[]{0, 0, 0}),
                        JsonUtils.parseOrDefault(context, object, "rotation", boolean[].class, new double[]{0, 0, 0})
                );
            } else {
                throw new JsonParseException("can't parse Locator : " + json);
            }
        }
    }
}
