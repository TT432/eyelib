package io.github.tt432.eyelib.molang.functions.utility;

import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.molang.MolangVariableScope;

@MolangFunctionHolder("math.die_roll_integer")
public class DieRollInteger extends MolangFunction {
    public DieRollInteger(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double evaluate(MolangVariableScope scope) {
        double i = 0.0D;
        double total = 0.0D;
        while (i < getArg(0, scope))
            total += Math.round(getArg(1, scope) + Math.random() * (getArg(2, scope) - getArg(1, scope)));
        return total;
    }
}