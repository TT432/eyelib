package io.github.tt432.eyelibmolang.mapping.api;

import io.github.tt432.eyelibmolang.MolangScope;

/**
 * Static bridge that lets mc-side code install a query runtime port.
 */
public final class MolangQueryRuntimeBridge {
    private static volatile MolangQueryRuntime runtime = MolangQueryRuntime.NOOP;

    private MolangQueryRuntimeBridge() {
    }

    public static void install(MolangQueryRuntime queryRuntime) {
        runtime = queryRuntime == null ? MolangQueryRuntime.NOOP : queryRuntime;
    }

    public static void reset() {
        runtime = MolangQueryRuntime.NOOP;
    }

    public static float actorCount() {
        return runtime.actorCount();
    }

    public static float timeOfDay() {
        return runtime.timeOfDay();
    }

    public static float moonPhase() {
        return runtime.moonPhase();
    }

    public static float distanceFromCamera(Object entity) {
        return runtime.distanceFromCamera(entity);
    }

    public static float resolvePartialTick(MolangScope scope) {
        if (scope.contains("variable.partial_tick")) {
            return scope.get("variable.partial_tick").asFloat();
        }

        return runtime.partialTick();
    }
}
