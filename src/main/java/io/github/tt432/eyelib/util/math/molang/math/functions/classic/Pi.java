package io.github.tt432.eyelib.util.math.molang.math.functions.classic;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Pi extends Function {
    public Pi(IValue[] values, String name) {
        super(values, name, 0);
    }

    public double get() {
        return Math.PI;
    }
}