package io.github.tt432.eyelib.molang.math;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;

import java.util.function.Function;

/**
 * Lazy override of Variable, to allow for deferred value calculation. <br>
 * Optimises rendering as values are not touched until needed (if at all)
 */
public class MolangVariable implements MolangValue {
    private final String name;
    private Function<MolangVariableScope, Double> valueFunc;

    public MolangVariable(String name, double value) {
        this(name, s -> value);
    }

    public MolangVariable(String name, Function<MolangVariableScope, Double> valueFunc) {
        this.name = name;
        this.valueFunc = valueFunc;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

    public void set(double value) {
        this.valueFunc = s -> value;
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        if (scope.containsCache(name))
            return scope.getValue(name);
        return this.valueFunc.apply(scope);
    }
}
