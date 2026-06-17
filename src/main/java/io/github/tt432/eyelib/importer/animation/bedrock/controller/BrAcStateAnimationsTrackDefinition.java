package io.github.tt432.eyelibimporter.animation.bedrock.controller;

import io.github.tt432.eyelibmolang.MolangValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrAcStateAnimationsTrackDefinition(
        BrAcStateTrackName name,
        Map<String, MolangValue> animations
) implements io.github.tt432.eyelibimporter.animation.bedrock.controller.BrAcStateTrackDefinition {
    public BrAcStateAnimationsTrackDefinition {
        animations = Collections.unmodifiableMap(new LinkedHashMap<>(animations));
    }
}
