package io.github.tt432.eyelib.util.math.molang.math.functions.classic;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Exp extends Function {
    public Exp(IValue[] values, String name) throws Exception {
        super(values, name, 1);
    }

    public double get() {
        return Math.exp(getArg(0));
    }
}