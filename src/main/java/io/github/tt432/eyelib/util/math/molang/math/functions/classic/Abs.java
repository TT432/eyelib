package io.github.tt432.eyelib.util.math.molang.math.functions.classic;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;


public class Abs extends Function {
    public Abs(IValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.abs(getArg(0));
    }
}
