package io.github.tt432.eyelib.util.math.molang.math.functions.limit;

import io.github.tt432.eyelib.util.math.MathE;
import io.github.tt432.eyelib.util.math.molang.math.IValue;
import io.github.tt432.eyelib.util.math.molang.math.functions.Function;

public class Clamp extends Function {
    public Clamp(IValue[] values, String name) {
        super(values, name, 3);
    }

    public double get() {
        return MathE.clamp(getArg(0), getArg(1), getArg(2));
    }
}