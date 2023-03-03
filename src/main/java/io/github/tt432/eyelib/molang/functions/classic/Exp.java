package io.github.tt432.eyelib.molang.functions.classic;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.exp")
public class Exp extends MolangFunction {
    public Exp(MolangValue[] values, String name) throws Exception {
        super(values, name, 1);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.exp(getArg(0, scope));
    }
}