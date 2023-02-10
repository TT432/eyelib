package io.github.tt432.eyelib.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import io.github.tt432.eyelib.util.molang.MolangValue;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author DustW
 */
@JsonAdapter(Value4.class)
@Data
public class Value4 implements JsonDeserializer<Value4> {
    private final MolangValue x;
    private final MolangValue y;
    private final MolangValue z;
    private final MolangValue w;

    @Override
    public Value4 deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        MolangValue[] deserialize = context.deserialize(json, MolangValue[].class);
        if (deserialize.length != 4)
            throw new JsonParseException("Value4 must be IValue[4]");
        return new Value4(deserialize[0], deserialize[1], deserialize[2], deserialize[3]);
    }
}
