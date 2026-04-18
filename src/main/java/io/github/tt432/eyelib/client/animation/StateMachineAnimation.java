package io.github.tt432.eyelib.client.animation;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Animation whose runtime behavior selects and advances named states.
 */
public interface StateMachineAnimation<D, S> extends Animation<D> {
    S initialState();

    Map<String, S> states();

    default @Nullable S state(String name) {
        return states().get(name);
    }
}
