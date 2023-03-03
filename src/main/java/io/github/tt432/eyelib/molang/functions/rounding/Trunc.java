package io.github.tt432.eyelib.molang.functions.rounding;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.trunc")
public class Trunc extends MolangFunction {
    public Trunc(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double evaluate(MolangVariableScope scope) {
        double value = getArg(0, scope);

        return (value < 0.0D) ? Math.ceil(value) : Math.floor(value);
    }
}