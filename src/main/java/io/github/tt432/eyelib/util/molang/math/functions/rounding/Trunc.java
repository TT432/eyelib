package io.github.tt432.eyelib.util.molang.math.functions.rounding;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class Trunc extends Function {
    public Trunc(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        double value = getArg(0);

        return (value < 0.0D) ? Math.ceil(value) : Math.floor(value);
    }
}