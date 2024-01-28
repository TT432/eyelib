package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public record BrAnimationControllers(
        Map<String, BrAnimationController> animation_controllers
) {
    private static final String EXCEPTION = "can't parse animation controller json file: %s .";

    public static BrAnimationControllers parse(String jsonName, JsonObject jsonObject) {
        if (!(jsonObject.get("format_version") instanceof JsonPrimitive jp) || !jp.getAsString().equals("1.19.0")) {
            throw new JsonParseException((EXCEPTION + "'format_version' not '1.19.0', please check the file.").formatted(jsonName));
        }

        if (!(jsonObject.get("animation_controllers") instanceof JsonObject jo)) {
            throw new JsonParseException((EXCEPTION + "can't found 'animation_controllers'.").formatted(jsonName));
        }

        Map<String, BrAnimationController> animationControllers = new HashMap<>();

        jo.asMap().forEach((k, v) -> {
            if (v instanceof JsonObject joIn && (!joIn.asMap().isEmpty())) {
                animationControllers.put(k, BrAnimationController.parse(jsonName, k, joIn));
            }
        });

        return new BrAnimationControllers(ImmutableMap.copyOf(animationControllers));
    }
}
