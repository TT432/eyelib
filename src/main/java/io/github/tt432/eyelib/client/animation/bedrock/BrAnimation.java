package io.github.tt432.eyelib.client.animation.bedrock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangScope;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@Slf4j
public record BrAnimation(
        Map<String, BrAnimationEntry> animations,
        MolangScope scope
) {

    public BrAnimation copy(MolangScope scope) {
        Map<String, BrAnimationEntry> copiedAnimations = new HashMap<>();
        animations.forEach((key, value) -> copiedAnimations.put(key, value.copy(scope)));

        return new BrAnimation(copiedAnimations, scope);
    }

    public static BrAnimation parse(String animationName, JsonObject jsonObject) {
        if (!(jsonObject.get("format_version") instanceof JsonPrimitive jp && jp.getAsString().equals("1.8.0"))) {
            throw new JsonParseException("can't parse %s, 'format_version' must be '1.8.0'".formatted(animationName));
        }

        final Map<String, BrAnimationEntry> animations = new HashMap<>();
        final MolangScope scope;

        scope = new MolangScope();

        if (!(jsonObject.get("animations") instanceof JsonObject jo)) {
            throw new JsonParseException("can't parse animation %s. not found 'animations'.".formatted(animationName));
        }

        for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
            animations.put(entry.getKey(), BrAnimationEntry.parse(scope, entry.getValue().getAsJsonObject()));
        }

        return new BrAnimation(animations, scope);
    }
}
