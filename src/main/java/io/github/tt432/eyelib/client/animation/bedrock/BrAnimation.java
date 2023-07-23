package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangScope;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author TT432
 */
@Slf4j
public class BrAnimation {
    Map<String, BrAnimationEntry> animations;
    MolangScope scope;

    public static BrAnimation parse(String animationName, JsonObject jsonObject) {
        if (!(jsonObject.get("format_version") instanceof JsonPrimitive jp && jp.getAsString().equals("1.8.0"))) {
            log.error("can't load {}, format version must be 1.8.0 .", animationName);
            return null;
        }

        BrAnimation result = new BrAnimation();

        result.scope = new MolangScope();

        result.animations = jsonObject.get("animations") instanceof JsonObject jo
                ?jo.asMap().entrySet().stream()
                    .map(entry -> Map.entry(entry.getKey(), BrAnimationEntry.parse(result.scope, entry.getValue().getAsJsonObject())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : new HashMap<>();

        return result;
    }
}
