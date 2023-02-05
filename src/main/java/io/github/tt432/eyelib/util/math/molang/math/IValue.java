package io.github.tt432.eyelib.util.math.molang.math;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import io.github.tt432.eyelib.util.math.molang.MolangException;
import io.github.tt432.eyelib.util.math.molang.MolangParser;

import java.lang.reflect.Type;

public interface IValue {
    double get();

    class Serializer implements JsonDeserializer<IValue> {
        @Override
        public IValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return parseExpression(MolangParser.getInstance(), json);
            } catch (MolangException e) {
                throw new RuntimeException(e);
            }
        }

        public static IValue parseExpression(MolangParser parser, JsonElement element) throws MolangException {
            if (element.getAsJsonPrimitive().isString()) {
                return parser.parseJson(element);
            } else {
                return new Constant(element.getAsDouble());
            }
        }
    }
}