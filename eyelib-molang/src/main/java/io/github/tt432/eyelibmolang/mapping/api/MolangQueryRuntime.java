package io.github.tt432.eyelibmolang.mapping.api;

/**
 * Narrow runtime port for environment-backed Molang query values.
 */
public interface MolangQueryRuntime {
    MolangQueryRuntime NOOP = new MolangQueryRuntime() {
        @Override
        public float actorCount() {
            return 0;
        }

        @Override
        public float timeOfDay() {
            return 0;
        }

        @Override
        public float moonPhase() {
            return 0;
        }

        @Override
        public float partialTick() {
            return 0;
        }

        @Override
        public float distanceFromCamera(Object entity) {
            return 0;
        }
    };

    float actorCount();

    float timeOfDay();

    float moonPhase();

    float partialTick();

    float distanceFromCamera(Object entity);
}
