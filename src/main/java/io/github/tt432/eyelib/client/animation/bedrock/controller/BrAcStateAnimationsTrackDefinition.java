package io.github.tt432.eyelib.client.animation.bedrock.controller;

import io.github.tt432.eyelibmolang.MolangValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record BrAcStateAnimationsTrackDefinition(
        BrAcStateTrackName name,
        Map<String, MolangValue> animations
) implements BrAcStateTrackDefinition {
    public BrAcStateAnimationsTrackDefinition {
        animations = Collections.unmodifiableMap(new LinkedHashMap<>(animations));
    }
}
