package io.github.tt432.eyelib.util.molang.math.functions.limit;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class Min extends Function {
    public Min(MolangValue[] values, String name) {
        super(values, name, 2);
    }

    public double get() {
        return Math.min(getArg(0), getArg(1));
    }
}