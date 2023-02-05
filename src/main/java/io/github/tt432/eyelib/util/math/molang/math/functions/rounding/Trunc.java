package io.github.tt432.eyelib.util.math.molang.math.functions.rounding;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Trunc extends Function {
    public Trunc(IValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        double value = getArg(0);

        return (value < 0.0D) ? Math.ceil(value) : Math.floor(value);
    }
}