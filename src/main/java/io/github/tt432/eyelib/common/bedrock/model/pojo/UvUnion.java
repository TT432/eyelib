package io.github.tt432.eyelib.common.bedrock.model.pojo;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.io.Serializable;
import java.lang.reflect.Type;

@JsonAdapter(UvUnion.Serializer.class)
public class UvUnion implements Serializable {
    public double[] boxUVCoords;
    public UvFaces faceUV;
    public boolean isBoxUV;

    protected static class Serializer implements JsonDeserializer<UvUnion> {
        @Override
        public UvUnion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            UvUnion result = new UvUnion();

            if (json.isJsonArray()) {
                result.isBoxUV = true;
                result.boxUVCoords = context.deserialize(json, double[].class);
            } else if (json.isJsonObject()) {
                result.isBoxUV = false;
                result.faceUV = context.deserialize(json, UvFaces.class);
            }

            return result;
        }
    }
}
