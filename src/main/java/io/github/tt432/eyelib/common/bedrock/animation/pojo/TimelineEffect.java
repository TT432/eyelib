package io.github.tt432.eyelib.common.bedrock.animation.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(TimelineEffect.class)
public class TimelineEffect implements JsonDeserializer<TimelineEffect> {
    MolangValue all;

    public void eval(MolangVariableScope scope) {
        all.evaluate(scope);
    }

    @Override
    public TimelineEffect deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        TimelineEffect result = new TimelineEffect();

        if (json.isJsonPrimitive()) {
            result.all = context.deserialize(json, MolangValue.class);
        } else if (json.isJsonArray()) {
            StringBuilder sb = new StringBuilder();
            json.getAsJsonArray().forEach(jele -> sb.append(jele.getAsString()));
            result.all = context.deserialize(new JsonPrimitive(sb.toString()), MolangValue.class);
        }

        return result;
    }
}
