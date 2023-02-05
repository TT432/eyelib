package io.github.tt432.eyelib.util.math.molang.math.functions.classic;

import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Mod extends Function {
    public Mod(IValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return getArg(0) % getArg(1);
    }
}