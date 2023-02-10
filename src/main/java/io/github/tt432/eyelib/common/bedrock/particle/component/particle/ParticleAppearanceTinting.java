package io.github.tt432.eyelib.common.bedrock.particle.component.particle;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.Value4;
import io.github.tt432.eyelib.util.molang.MolangValue;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author DustW
 */
@JsonAdapter(ParticleAppearanceTinting.class)
@ParticleComponentHolder("minecraft:particle_appearance_tinting")
public class ParticleAppearanceTinting extends ParticleComponent implements JsonDeserializer<ParticleAppearanceTinting> {
    Value4 color;
    MolangValue interpolant;
    /**
     * time -> color
     */
    Map<Double, Integer> gradient;

    Mode mode;

    public enum Mode {
        EXPRESSION,
        GRADIENT
    }

    @Override
    public ParticleAppearanceTinting deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        ParticleAppearanceTinting result = new ParticleAppearanceTinting();
        JsonObject object = json.getAsJsonObject();
        JsonElement colorJson = object.get("color");

        if (colorJson.isJsonArray()) {
            result.color = context.deserialize(colorJson, Value4.class);
            result.mode = Mode.EXPRESSION;
        } else if (colorJson.isJsonObject()) {
            JsonObject colorJsonObject = colorJson.getAsJsonObject();
            result.interpolant = context.deserialize(colorJsonObject.get("interpolant"), MolangValue.class);
            result.gradient = context.deserialize(colorJsonObject.get("gradient"),
                    TypeToken.getParameterized(Map.class, Double.class, Integer.class).getType());
            result.mode = Mode.GRADIENT;
        }

        return result;
    }
}
