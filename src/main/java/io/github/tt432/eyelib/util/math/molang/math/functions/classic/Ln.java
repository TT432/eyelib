package io.github.tt432.eyelib.util.math.molang.math.functions.classic;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Ln extends Function {
    public Ln(IValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.log(getArg(0));
    }
}