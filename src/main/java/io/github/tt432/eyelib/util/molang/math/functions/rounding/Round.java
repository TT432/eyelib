package io.github.tt432.eyelib.util.molang.math.functions.rounding;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class Round extends Function {
    public Round(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.round(getArg(0));
    }
}