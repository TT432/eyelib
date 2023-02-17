package io.github.tt432.eyelib.common.bedrock.particle.component.particle.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.math.Constant;
import io.github.tt432.eyelib.molang.util.Value2;
import io.github.tt432.eyelib.util.json.JsonUtils;
import io.github.tt432.eyelib.util.math.Vec2d;
import io.github.tt432.eyelib.util.math.Vec4d;

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

    /**
     * u0 u1 v0 v1
     *
     * @param scope scope
     * @return uv
     */
    public Vec4d getUV(MolangVariableScope scope) {
        if (mode == Mode.STATIC) {
            return staticUv(scope);
        } else if (mode == Mode.ANIMATED) {
            return flipbook.getUv(scope, width, height);
        }

        return new Vec4d(0, 0, 0, 0);
    }

    Vec4d staticUv(MolangVariableScope scope) {
        Vec2d startVec = start.evaluate(scope);
        Vec2d sizeVec = size.evaluate(scope);

        if (width == 1 && height == 1) {
            return new Vec4d(
                    startVec.getX(),
                    startVec.getX() + sizeVec.getX(),
                    startVec.getY(),
                    startVec.getY() + sizeVec.getY()
            );
        } else {
            return new Vec4d(
                    startVec.getX() / width,
                    (startVec.getX() + sizeVec.getX()) / width,
                    startVec.getY() / height,
                    (startVec.getY() + sizeVec.getY()) / height
            );
        }
    }

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
            result.mode = Mode.STATIC;
        } else {
            result.flipbook = context.deserialize(object.get("flipbook"), ABFlipbook.class);
            result.mode = Mode.ANIMATED;
        }

        return result;
    }

    public enum Mode {
        STATIC,
        ANIMATED
    }
}
