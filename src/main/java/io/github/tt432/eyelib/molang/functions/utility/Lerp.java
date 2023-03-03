package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.math.MathE;

@MolangFunctionHolder("math.lerp")
public class Lerp extends MolangFunction {
    public Lerp(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double evaluate(MolangVariableScope scope) {
        return MathE.lerp(getArg(0, scope), getArg(1, scope), getArg(2, scope));
    }
}