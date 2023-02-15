package io.github.tt432.eyelib.common.bedrock.particle.component.particle;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.util.Value4;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;
import io.github.tt432.eyelib.util.Color;
import io.github.tt432.eyelib.util.math.MathE;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.TreeMap;

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
    TreeMap<Double, Integer> gradient;

    Mode mode;

    public int getColor(MolangVariableScope scope) {
        if (mode == Mode.EXPRESSION) {
            float r = (float) color.getX().evaluate(scope);
            float g = (float) color.getY().evaluate(scope);
            float b = (float) color.getZ().evaluate(scope);
            float a = (float) color.getW().evaluate(scope);

            return Color.ofRGBA(r, g, b, a).getColor();
        } else {
            double time = interpolant.evaluate(scope);

            Map.Entry<Double, Integer> before = gradient.lowerEntry(time);
            Map.Entry<Double, Integer> after = gradient.ceilingEntry(time);

            if (before == null)
                return after.getValue();

            if (after == null)
                return before.getValue();

            return (int) MathE.lerp(before.getValue(), after.getValue(),
                    MathE.getWeight(before.getKey(), after.getKey(), time));
        }
    }

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
            result.gradient = new TreeMap<>();
            colorJsonObject.get("gradient").getAsJsonObject().entrySet().forEach(e ->
                    result.gradient.put(Double.parseDouble(e.getKey()),
                            Integer.parseUnsignedInt(e.getValue().getAsString().substring(1), 16)));
            result.mode = Mode.GRADIENT;
        }

        return result;
    }
}
