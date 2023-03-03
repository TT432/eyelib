package io.github.tt432.eyelib.molang.functions.limit;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.max")
public class Max extends MolangFunction {
    public Max(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.max(getArg(0, scope), getArg(1, scope));
    }
}