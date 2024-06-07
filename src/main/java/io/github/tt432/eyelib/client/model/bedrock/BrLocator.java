package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import io.github.tt432.eyelib.util.math.EyeMath;
import lombok.Getter;
import org.joml.Vector3f;

/**
 * @author TT432
 */
@Getter
public class BrLocator {
    public static final String NULL_OBJ_PREFIX = "_null_";

    private static final Gson gson = new Gson();

    Vector3f offset;
    Vector3f rotation;
    boolean ignoreInheritedScale;

    String key;
    boolean isNullObject;

    public static BrLocator parse(String key, JsonElement value) {
        BrLocator result = new BrLocator();

        if (key.startsWith(NULL_OBJ_PREFIX)) {
            result.isNullObject = true;
            result.key = key.substring(NULL_OBJ_PREFIX.length());
        } else {
            result.isNullObject = false;
            result.key = key;
        }

        if (value instanceof JsonArray ja) {
            result.offset = new Vector3f(gson.fromJson(ja, float[].class));
        } else if (value instanceof JsonObject jo) {
            result.offset = jo.get("offset") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
            result.rotation = jo.get("rotation") instanceof JsonArray ja ? new Vector3f(gson.fromJson(ja, float[].class)) : new Vector3f(0);
            result.ignoreInheritedScale = jo.get("ignore_inherited_scale") instanceof JsonPrimitive jp && jp.getAsBoolean();
        }

        result.offset.div(16).mul(-1, 1, 1);
        result.rotation.mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);

        return result;
    }
}
