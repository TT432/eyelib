package io.github.tt432.eyelib.client.animation;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Immutable state-machine animation definition.
 */
public interface StateMachineAnimationDefinition<S> {
    String name();

    S initialState();

    Map<String, S> states();

    default @Nullable S state(String name) {
        return states().get(name);
    }
}
