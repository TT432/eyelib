package io.github.tt432.eyelib.common.bedrock.particle;

import io.github.tt432.eyelib.molang.MolangVariableScope;

/**
 * @author DustW
 */
public interface EvaluateAble {
    default void evaluateStart(MolangVariableScope scope) {
        // need child impl
    }

    default void evaluateLoopStart(MolangVariableScope scope) {
        // need child impl
    }

    default void evaluatePerUpdate(MolangVariableScope scope) {
        // need child impl
    }

    default void evaluatePerEmit(MolangVariableScope scope) {
        // need child impl
    }
}
