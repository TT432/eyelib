package io.github.tt432.eyelib.molang.functions.classic;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.sqrt")
public class Sqrt extends MolangFunction {
    public Sqrt(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.sqrt(getArg(0, scope));
    }
}