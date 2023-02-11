package io.github.tt432.eyelib.common.bedrock.particle.component.particle.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.util.Value2;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.molang.math.Constant;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(ABUV.class)
public class ABUV implements JsonDeserializer<ABUV> {
    /**
     * specifies the assumed texture width/height
     * defaults to 1
     * when set to 1, UV's work just like normalized UV's
     * when set to the texture width/height, this works like texels
     */
    @SerializedName("texture_width")
    int width;
    @SerializedName("texture_height")
    int height;

    Mode mode;

    /* ------------------ static --------------------------- */

    /**
     * Assuming the specified texture width and height, use these
     * uv coordinates.
     * evaluated every frame
     */
    @SerializedName("uv")
    Value2 start;
    @SerializedName("uv_size")
    Value2 size;

    /* ------------------- animated ------------------------ */

    ABFlipbook flipbook;

    @Override
    public ABUV deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ABUV result = new ABUV();

        JsonObject object = json.getAsJsonObject();
        result.width = JsonUtils.parseOrDefault(context, object, "texture_width", int.class, 1);
        result.height = JsonUtils.parseOrDefault(context, object, "texture_height", int.class, 1);

        if (object.has("uv_size")) {
            result.start = JsonUtils.parseOrDefault(context, object, "uv", Value2.class,
                    new Value2(new Constant(0), new Constant(0)));
            result.size = JsonUtils.parseOrDefault(context, object, "uv_size", Value2.class,
                    new Value2(new Constant(0), new Constant(0)));
            mode = Mode.STATIC;
        } else {
            result.flipbook = context.deserialize(object.get("flipbook"), ABFlipbook.class);
            mode = Mode.ANIMATED;
        }

        return result;
    }

    public enum Mode {
        STATIC,
        ANIMATED
    }
}
