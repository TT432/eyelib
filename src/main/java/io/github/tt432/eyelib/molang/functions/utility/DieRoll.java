package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.MolangVariableScope;

@MolangFunctionHolder("math.die_roll")
public class DieRoll extends MolangFunction {
    public DieRoll(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double evaluate(MolangVariableScope scope) {
        double i = 0.0D;
        double total = 0.0D;
        while (i < getArg(0, scope))
            total += (Math.random() * (getArg(2, scope)) - getArg(2, scope));
        return total;
    }
}