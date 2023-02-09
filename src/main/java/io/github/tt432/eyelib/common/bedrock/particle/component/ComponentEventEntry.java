package io.github.tt432.eyelib.common.bedrock.particle.component;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author DustW
 */
@JsonAdapter(ComponentEventEntry.Serializer.class)
public class ComponentEventEntry {
    List<String> values;

    protected static class Serializer implements JsonDeserializer<ComponentEventEntry> {
        @Override
        public ComponentEventEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ComponentEventEntry result = new ComponentEventEntry();

            if (json.isJsonPrimitive()) {
                result.values = Collections.singletonList(json.getAsString());
            } else if (json.isJsonArray()) {
                result.values = new ArrayList<>();
                JsonArray asJsonArray = json.getAsJsonArray();
                asJsonArray.forEach(je -> result.values.add(je.getAsString()));
            }

            return result;
        }
    }
}
