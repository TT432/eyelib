package io.github.tt432.eyelib.client.model.bedrock;

import com.google.gson.*;
import io.github.tt432.eyelib.util.math.EyeMath;
import org.joml.Vector3f;

/**
 * @author TT432
 */
public record BrLocator(
        Vector3f offset,
        Vector3f rotation,
        boolean ignoreInheritedScale,
        String key,
        boolean isNullObject
) {
    public static final String NULL_OBJ_PREFIX = "_null_";

    private static final Gson gson = new Gson();

    public static BrLocator parse(String key, JsonElement value) {
        boolean isNullObject;
        String rKey;

        if (key.startsWith(NULL_OBJ_PREFIX)) {
            isNullObject = true;
            rKey = key.substring(NULL_OBJ_PREFIX.length());
        } else {
            isNullObject = false;
            rKey = key;
        }

        Vector3f offset = new Vector3f();
        Vector3f rotation = new Vector3f();
        boolean ignoreInheritedScale = false;

        if (value instanceof JsonArray ja) {
            offset.set(gson.fromJson(ja, float[].class));
        } else if (value instanceof JsonObject jo) {
            if (jo.get("offset") instanceof JsonArray ja) offset.set(gson.fromJson(ja, float[].class));
            if (jo.get("rotation") instanceof JsonArray ja) rotation.set(gson.fromJson(ja, float[].class));
            ignoreInheritedScale = jo.get("ignore_inherited_scale") instanceof JsonPrimitive jp && jp.getAsBoolean();
        }

        offset.div(16).mul(-1, 1, 1);
        rotation.mul(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);

        return new BrLocator(offset, rotation, ignoreInheritedScale, rKey, isNullObject);
    }
}
