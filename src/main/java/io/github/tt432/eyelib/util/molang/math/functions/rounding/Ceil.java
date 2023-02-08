package io.github.tt432.eyelib.util.molang.math.functions.rounding;

import io.github.tt432.eyelib.util.molang.MolangValue;
import io.github.tt432.eyelib.util.molang.math.functions.Function;

public class Ceil extends Function {
    public Ceil(MolangValue[] values, String name) {
        super(values, name, 1);
    }

    public double get() {
        return Math.ceil(getArg(0));
    }
}