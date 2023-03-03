package io.github.tt432.eyelib.molang.functions.rounding;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.ceil")
public class Ceil extends MolangFunction {
    public Ceil(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.ceil(getArg(0, scope));
    }
}