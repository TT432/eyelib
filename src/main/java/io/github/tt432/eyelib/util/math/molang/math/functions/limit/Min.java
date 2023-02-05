package io.github.tt432.eyelib.util.math.molang.math.functions.limit;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Min extends Function {
    public Min(IValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.min(getArg(0), getArg(1));
    }
}