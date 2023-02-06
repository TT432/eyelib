package io.github.tt432.eyelib.util.molang.math.functions.utility;

import io.github.tt432.eyelib.util.molang.math.IValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class DieRoll extends Function {
    public DieRoll(IValue[] values, String name) {
        super(values, name, 3);
    }

    public double get() {
        double i = 0.0D;
        double total = 0.0D;
        while (i < getArg(0))
            total += (Math.random() * (getArg(2)) - getArg(2));
        return total;
    }
}