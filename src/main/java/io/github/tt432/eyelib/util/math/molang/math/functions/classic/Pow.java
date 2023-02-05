package io.github.tt432.eyelib.util.math.molang.math.functions.classic;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Pow extends Function {
    public Pow(IValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.pow(getArg(0), getArg(1));
    }
}