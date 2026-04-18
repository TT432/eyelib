package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelibmolang.MolangValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record BrAcStateTransitionsTrackDefinition(
        BrAcStateTrackName name,
        Map<String, MolangValue> transitions
) implements BrAcStateTrackDefinition {
    public BrAcStateTransitionsTrackDefinition {
        transitions = Collections.unmodifiableMap(new LinkedHashMap<>(transitions));
    }
}
