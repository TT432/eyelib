package io.github.tt432.eyelib.molang.functions.limit;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.min")
public class Min extends MolangFunction {
    public Min(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.min(getArg(0, scope), getArg(1, scope));
    }
}