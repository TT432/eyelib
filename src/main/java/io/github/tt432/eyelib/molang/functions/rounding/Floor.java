package io.github.tt432.eyelib.molang.functions.rounding;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.floor")
public class Floor extends MolangFunction {
    public Floor(MolangValue[] values, String name) throws Exception {
        super(values, name, 1);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.floor(getArg(0, scope));
    }
}