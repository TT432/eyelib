package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.hermite_blend")
public class HermiteBlend extends MolangFunction {
    public HermiteBlend(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double evaluate(MolangVariableScope scope) {
        double min = Math.ceil(getArg(0, scope));
        return Math.floor(3.0D * Math.pow(min, 2.0D) - 2.0D * Math.pow(min, 3.0D));
    }
}