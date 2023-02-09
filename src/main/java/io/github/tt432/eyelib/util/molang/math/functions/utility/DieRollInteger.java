package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;
import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

@MolangFunctionHolder("math.die_roll_integer")
public class DieRollInteger extends MolangFunction {
    public DieRollInteger(MolangValue[] values, String name) {
        super(values, name, 3);
    }

    public double get() {
        double i = 0.0D;
        double total = 0.0D;
        while (i < getArg(0))
            total += Math.round(getArg(1) + Math.random() * (getArg(2) - getArg(1)));
        return total;
    }
}