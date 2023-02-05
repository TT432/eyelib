package software.bernie.geckolib3.geo.raw.pojo;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.io.IOException;
import java.lang.reflect.Type;

@JsonAdapter(FormatVersion.Serializer.class)
public enum FormatVersion {
	VERSION_1_12_0, VERSION_1_14_0, VERSION_1_8_0;

	public String toValue() {
		return switch (this) {
			case VERSION_1_12_0 -> "1.12.0";
			case VERSION_1_14_0 -> "1.14.0";
			case VERSION_1_8_0 -> "1.8.0";
		};
	}

	public static FormatVersion forValue(String value) throws IOException {
		if (value.equals("1.12.0"))
			return VERSION_1_12_0;
		if (value.equals("1.14.0"))
			return VERSION_1_14_0;
		if (value.equals("1.8.0"))
			return VERSION_1_8_0;
		throw new IOException("Cannot deserialize FormatVersion: " + value);
	}

	protected static class Serializer implements JsonSerializer<FormatVersion>, JsonDeserializer<FormatVersion> {
		@Override
		public FormatVersion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				return FormatVersion.forValue(json.getAsString());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public JsonElement serialize(FormatVersion src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(src.toValue());
		}
	}
}
