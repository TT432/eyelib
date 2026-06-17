package io.github.tt432.eyelib.molang.mapping.api;

/**
 * 环境相关 Molang 查询值的运行时适配接口。
 *
 * @author TT432
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