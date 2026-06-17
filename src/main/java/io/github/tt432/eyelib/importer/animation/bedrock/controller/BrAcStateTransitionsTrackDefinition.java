package io.github.tt432.eyelib.importer.animation.bedrock.controller;

import io.github.tt432.eyelib.molang.MolangValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrAcStateTransitionsTrackDefinition(
        io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackName name,
        Map<String, MolangValue> transitions
) implements io.github.tt432.eyelib.importer.animation.bedrock.controller.BrAcStateTrackDefinition {
    public BrAcStateTransitionsTrackDefinition {
        transitions = Collections.unmodifiableMap(new LinkedHashMap<>(transitions));
    }
}
