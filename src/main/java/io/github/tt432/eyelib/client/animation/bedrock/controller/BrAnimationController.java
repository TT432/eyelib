package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.molang.MolangScope;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author TT432
 */
@AllArgsConstructor
public class BrAnimationController {
    final String name;
    final BrAcState initialState;
    final Map<String, BrAcState> states;

    final MolangScope scope;

    private static final String EXCEPTION = "can't parse animation controller json file: %s .";

    public static BrAnimationController parse(String jsonName, JsonObject jsonObject) {
        if (!(jsonObject.get("format_version") instanceof JsonPrimitive jp) || !jp.getAsString().equals("1.19.0")) {
            throw new JsonParseException((EXCEPTION + "'format_version' not '1.19.0', please check the file.").formatted(jsonName));
        }

        if (!(jsonObject.get("animation_controllers") instanceof JsonObject jo)) {
            throw new JsonParseException((EXCEPTION + "can't found 'animation_controllers'.").formatted(jsonName));
        }

        Map.Entry<String, JsonElement> animationControllers = jo.entrySet().stream().findFirst().orElse(null);

        if (animationControllers == null) {
            throw new JsonParseException((EXCEPTION + "'animation_controllers' not have any entry.").formatted(jsonName));
        }

        final String name;
        final BrAcState initialState;
        final Map<String, BrAcState> states = new HashMap<>();
        final MolangScope scope;

        name = animationControllers.getKey();
        scope = new MolangScope();

        if (!(animationControllers.getValue() instanceof JsonObject animCtrlEntryJson)) {
            throw new JsonParseException((EXCEPTION + "The file don't have entry 'animation_controllers'").formatted(jsonName));
        }

        if (!(animCtrlEntryJson.get("states") instanceof JsonObject stateJson)) {
            throw new JsonParseException((EXCEPTION + "entry 'states' dose not JsonObject.").formatted(jsonName));
        }

        for (Map.Entry<String, JsonElement> singleState : stateJson.asMap().entrySet()) {
            try {
                states.put(singleState.getKey(), BrAcState.parse(scope, singleState.getValue()));
            } catch (JsonParseException jsonParseException) {
                throw new JsonParseException("can't parse controller json: %s".formatted(jsonName), jsonParseException);
            }
        }

        if (!(animCtrlEntryJson.get("initial_state") instanceof JsonPrimitive isj)) {
            throw new JsonParseException((EXCEPTION + "The file don't have field 'initial_state'").formatted(jsonName));
        }

        initialState = states.get(isj.getAsString());

        return new BrAnimationController(name, initialState, states, scope);
    }
}
