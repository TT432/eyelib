package io.github.tt432.eyelib.molang.math.functions.limit;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.math.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.math.MathE;
import io.github.tt432.eyelib.molang.MolangVariableScope;

@MolangFunctionHolder("math.clamp")
public class Clamp extends MolangFunction {
    public Clamp(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double evaluate(MolangVariableScope scope) {
        return MathE.clamp(getArg(0, scope), getArg(1, scope), getArg(2, scope));
    }
}