package io.github.tt432.eyelib.molang.functions.classic;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.atan2")
public class ATan2 extends MolangFunction {
    public ATan2(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double evaluate(MolangVariableScope scope) {
        return Math.atan2(getArg(0, scope), getArg(1, scope));
    }
}