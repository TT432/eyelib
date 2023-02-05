package io.github.tt432.eyelib.util.math.molang.math.functions.limit;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Max extends Function {
    public Max(IValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.max(getArg(0), getArg(1));
    }
}