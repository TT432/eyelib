package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelib.client.animation.StateMachineAnimationDefinition;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcState;
import io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAnimationControllerSchema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record BrAnimationControllerDefinition(
        String name,
        BrAcStateDefinition initialState,
        Map<String, BrAcStateDefinition> states
) implements StateMachineAnimationDefinition<BrAcStateDefinition> {
    public BrAnimationControllerDefinition {
        states = Collections.unmodifiableMap(new LinkedHashMap<>(states));
    }

    public static BrAnimationControllerDefinition fromSchema(String name, BrAnimationControllerSchema schema) {
        LinkedHashMap<String, BrAcStateDefinition> definitions = new LinkedHashMap<>();
        schema.states().forEach((stateName, state) -> definitions.put(stateName, BrAcStateDefinition.fromSchema(state)));

        BrAcStateDefinition initial = definitions.get(schema.initialState());
        if (initial == null) {
            initial = definitions.get("default");
        }
        if (initial == null) {
            initial = BrAcStateDefinition.fromSchema(new BrAcState(Map.of(), io.github.tt432.eyelibmolang.MolangValue.ZERO,
                    io.github.tt432.eyelibmolang.MolangValue.ZERO, java.util.List.of(), java.util.List.of(), Map.of(), 0F, false));
        }
        return new BrAnimationControllerDefinition(name, initial, definitions);
    }

    public BrAnimationControllerSchema toSchema() {
        LinkedHashMap<String, BrAcState> schemaStates = new LinkedHashMap<>();
        states.forEach((stateName, state) -> schemaStates.put(stateName, state.toSchema()));
        for (Map.Entry<String, BrAcStateDefinition> entry : states.entrySet()) {
            if (entry.getValue().equals(initialState)) {
                return new BrAnimationControllerSchema(entry.getKey(), schemaStates);
            }
        }
        return new BrAnimationControllerSchema("default", schemaStates);
    }
}
