package software.bernie.geckolib3.util.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
 * Copyright: DerToaster98 - 13.06.2022
 * 
 * needed for emissive texture
 * 
 * Originally developed for chocolate quest repoured
 */
public class JsonUtil {

	public static <T extends JsonElement> Stream<T> stream(JsonArray jsonArray, Class<T> jsonClass) {
		return IntStream.range(0, jsonArray.size())
				.mapToObj(jsonArray::get)
				.filter(jsonClass::isInstance)
				.map(jsonClass::cast);
	}

}
