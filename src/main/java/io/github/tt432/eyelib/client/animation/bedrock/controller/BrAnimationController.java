package io.github.tt432.eyelib.client.animation.bedrock.controller;

import com.google.gson.JsonElement;
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
public class BrAnimationController {
    String name;
    String initialState;
    Map<String, BrAcState> states;

    MolangScope scope;

    public static BrAnimationController parse(String animationControllerName, JsonObject jsonObject) {
        if (!(jsonObject.get("format_version") instanceof JsonPrimitive jp) || !jp.getAsString().equals("1.19.0")) {
            log.error("can't load animation controller {}", animationControllerName);
            return null;
        }

        JsonElement animationControllersJson = jsonObject.get("animation_controllers");

        if (animationControllersJson instanceof JsonObject jo ) {
            return jo.asMap().entrySet().stream().findFirst().map(entry -> {
                BrAnimationController result = new BrAnimationController();

                result.name = entry.getKey();
                result.scope = new MolangScope();

                if (entry.getValue() instanceof JsonObject animCtrlEntryJson) {
                    result.initialState =animCtrlEntryJson.get("initial_state") instanceof JsonPrimitive isj ? isj.getAsString() : "default";
                    result.states = animCtrlEntryJson.get("states") instanceof JsonObject stateJson
                            ? stateJson.asMap().entrySet().stream()
                                .map(stateEntry -> Map.entry(stateEntry.getKey(), BrAcState.parse(result.scope,stateEntry.getValue())))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                            : new HashMap<>();
                }

                return result;
            }).orElse(null);
        }

        return null;
    }
}
