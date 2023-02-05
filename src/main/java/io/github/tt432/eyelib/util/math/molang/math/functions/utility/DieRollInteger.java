package io.github.tt432.eyelib.util.math.molang.math.functions.utility;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class DieRollInteger extends Function {
    public DieRollInteger(IValue[] values, String name) {
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