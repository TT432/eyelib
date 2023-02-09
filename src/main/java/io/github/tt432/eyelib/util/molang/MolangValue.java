package io.github.tt432.eyelib.util.molang;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public interface MolangValue {
    double get();

    default String getAsString() {
        return String.valueOf(get());
    }

    class Serializer implements JsonDeserializer<MolangValue> {
        @Override
        public MolangValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return MolangParser.getInstance().parseJson(json);
            } catch (MolangException e) {
                throw new JsonParseException(e);
            }
        }
    }
}