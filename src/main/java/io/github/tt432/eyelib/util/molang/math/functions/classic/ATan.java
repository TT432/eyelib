package io.github.tt432.eyelib.util.molang.math.functions.classic;

import io.github.tt432.eyelib.util.molang.math.IValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;


public class ATan extends Function {
    public ATan(IValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.atan(getArg(0));
    }
}
