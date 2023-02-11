package io.github.tt432.eyelib.molang.math.functions.classic;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.math.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.MolangVariableScope;

@MolangFunctionHolder("math.pow")
public class Pow extends MolangFunction {
    public Pow(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.pow(getArg(0, scope), getArg(1, scope));
    }
}