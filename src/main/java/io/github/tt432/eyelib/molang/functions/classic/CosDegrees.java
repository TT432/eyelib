package io.github.tt432.eyelib.molang.functions.classic;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.MolangVariableScope;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

@MolangFunctionHolder("math.cos")
public class CosDegrees extends MolangFunction {
    public CosDegrees(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    @Override
    public double evaluate(MolangVariableScope scope) {
        return Math.cos(this.getArg(0, scope) / 180 * Math.PI);
    }
}
