package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangSystemScope;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
public record BrAnimationController(
        String name,
        BrAcState initialState,
        Map<String, BrAcState> states
) {
    private static final String EXCEPTION = "can't parse animation controller json file: %s .";

    public static BrAnimationController parse(String jsonName, String name, JsonObject animCtrlEntryJson) {
        final BrAcState initialState;
        final Map<String, BrAcState> states = new HashMap<>();

        if (!(animCtrlEntryJson.get("states") instanceof JsonObject stateJson)) {
            throw new JsonParseException((EXCEPTION + "entry 'states' dose not JsonObject.").formatted(jsonName));
        }

        for (Map.Entry<String, JsonElement> singleState : stateJson.asMap().entrySet()) {
            try {
                states.put(singleState.getKey(), BrAcState.parse(MolangSystemScope.ANIMATIONS, singleState.getValue()));
            } catch (JsonParseException jsonParseException) {
                throw new JsonParseException("can't parse controller json: %s".formatted(jsonName), jsonParseException);
            }
        }

        if (animCtrlEntryJson.get("initial_state") instanceof JsonPrimitive isj) {
            initialState = states.get(isj.getAsString());
        } else {
            initialState = states.get("default");
        }

        return new BrAnimationController(name, initialState, states);
    }
}
