package io.github.tt432.eyelib.molang;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public interface MolangValue {
    double evaluate(MolangVariableScope scope);

    default double evaluateWithCache(String name, MolangVariableScope scope) {
        double evaluate = evaluate(scope);
        scope.cache.put(name, () -> evaluate);
        return evaluate;
    }

    default String asString(MolangVariableScope scope) {
        return String.valueOf(evaluate(scope));
    }

    class Serializer implements JsonDeserializer<MolangValue> {
        @Override
        public MolangValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return MolangParser.getInstance().parseJson(json, MolangParser.scopeStack.last());
            } catch (MolangException e) {
                throw new JsonParseException(e);
            }
        }
    }
}